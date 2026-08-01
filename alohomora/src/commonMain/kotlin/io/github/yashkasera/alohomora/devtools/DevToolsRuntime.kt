package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.AuthChallengeMessage
import io.github.yashkasera.alohomora.common.AuthFailureMessage
import io.github.yashkasera.alohomora.common.AuthOtpRequiredMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseSnapshotMessage
import io.github.yashkasera.alohomora.common.DatabaseSnapshotPayload
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.PrefsSnapshotMessage
import io.github.yashkasera.alohomora.common.PrefsSnapshotPayload
import io.github.yashkasera.alohomora.common.RequestClearMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseTableMessage
import io.github.yashkasera.alohomora.common.RequestInitialStateMessage
import io.github.yashkasera.alohomora.common.ReplayResultMessage
import io.github.yashkasera.alohomora.common.RequestPrefValueMessage
import io.github.yashkasera.alohomora.common.RequestReplayTraceMessage
import io.github.yashkasera.alohomora.common.StreamApiLogMessage
import io.github.yashkasera.alohomora.common.StreamEventMessage
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.replay.ReplayOutcome
import io.github.yashkasera.alohomora.replay.TraceReplayRegistry
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.CoroutineExceptionHandler

internal object DevToolsDefaults {
    const val DEFAULT_PORT: Int = 53999
    const val EVENT_SNAPSHOT_LIMIT: Int = 500
    const val API_LOG_SNAPSHOT_LIMIT: Int = 200
    const val STREAM_BUFFER_CAPACITY: Int = 1024

    /**
     * How long to wait for a client's token probe before displaying the OTP regardless.
     * Long enough that a token-bearing client never flashes the prompt on a local socket, short
     * enough that a client which never probes is not left waiting.
     */
    const val OTP_REVEAL_GRACE_MILLIS: Long = 400
}

@OptIn(ExperimentalAtomicApi::class)
internal class DevToolsRuntime(
    private val telemetryRepository: TelemetryRepository,
    private val database: AlohomoraDb,
    private val preferencesInspector: DevToolsPreferencesInspector,
    private val server: DevToolsTcpServer,
    private val appDatabaseProvider: DevToolsAppDatabaseProvider,
    private val trustStore: DevToolsTrustStore,
) {
    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        println("[Alohomora] DevTools uncaught exception: ${t.message}")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private val sequenceCounter = AtomicLong(1)

    // @Volatile: written from attachClient (IO), from DevToolsConnection.close() (the
    // connection scope) and from stop() (whatever thread toggled the UI switch). Plain vars
    // let two rapid connects leave an orphaned connection that stop() can no longer reach.
    @Volatile
    private var activeConnection: DevToolsConnection? = null

    private val databaseInspector = DevToolsDatabaseInspector(database, appDatabaseProvider)

    @Volatile
    private var defaultDatabaseName: String? = null

    /** Port to rebind to on foreground. Null when the server is not meant to be running. */
    @Volatile
    private var listenPort: Int? = null

    @Volatile
    private var isObservingOtp = false
    private val _serverState = MutableStateFlow(DevToolsServerState())
    val serverState: StateFlow<DevToolsServerState> = _serverState.asStateFlow()

    fun start(port: Int = DevToolsDefaults.DEFAULT_PORT): Boolean {
        if (!isDebugBuild) return false
        // Guarded: start() used to add a fresh collector on every call, so toggling the server
        // off and on left two observers driving the same prompt.
        if (!isObservingOtp) {
            isObservingOtp = true
            observePendingOtp()
        }
        AppLifecycle.observeForeground(::rebindAfterForeground)
        listenPort = port
        bind(port)
        _serverState.value = _serverState.value.copy(
            isRunning = true,
            port = port,
            lastError = null,
        )
        return true
    }

    /** Binds the listening socket. Split out so the foreground handler can rebind. */
    private fun bind(port: Int) {
        // server.start is non-blocking (bind runs on IO dispatcher); always returns true optimistically.
        server.start(port) { socket ->
            scope.launch {
                attachClient(socket)
            }
        }
    }

    /**
     * Rebinds after the app returns to the foreground.
     *
     * The stale connection is closed first, and that is the part that matters. While suspended,
     * the device never notices the desktop has gone — [activeConnection] stays non-null, and
     * [attachClient] rejects any new client while one is attached. So without this the desktop
     * would retry forever against a device that refuses every attempt, until the old reader
     * eventually noticed EOF. Closing it up front makes the resume immediate.
     */
    private fun rebindAfterForeground() {
        if (!_serverState.value.isRunning) return
        val port = listenPort ?: return
        activeConnection?.close()
        activeConnection = null
        _serverState.value = _serverState.value.copy(hasClient = false, pendingOtp = null)
        server.stop()
        bind(port)
    }

    fun stop() {
        AppLifecycle.stopObserving()
        listenPort = null
        activeConnection?.close()
        activeConnection = null
        server.stop()
        _serverState.value = _serverState.value.copy(
            isRunning = false,
            hasClient = false,
            port = null,
        )
    }

    private suspend fun attachClient(socket: DevToolsSocket) {
        // Reject rather than evict. The previous behaviour was last-writer-wins, which let
        // any unauthenticated peer repeatedly kick the developer's live session off (and
        // reset the displayed OTP each time) simply by reconnecting.
        if (activeConnection != null) {
            println("[Alohomora] DevTools already has a client; rejecting additional connection.")
            socket.close()
            return
        }
        val connection = DevToolsConnection(socket)
        activeConnection = connection
        _serverState.value = _serverState.value.copy(
            hasClient = true,
            lastError = null,
            pendingOtp = null,
            // Consent is per pairing. Carrying a previous "yes" forward would silently persist
            // credentials for a desktop the user never agreed to remember.
            rememberDevice = false,
        )
        connection.start()
    }

    private fun nextSequence(): Long = sequenceCounter.fetchAndAdd(1)

    /** Records the user's answer to "remember this computer" for the pairing in progress. */
    fun setRememberDevice(remember: Boolean) {
        _serverState.value = _serverState.value.copy(rememberDevice = remember)
    }

    /**
     * Mirrors [DevToolsServerState.pendingOtp] onto the platform prompt.
     *
     * Driven off the same state the console screen renders so the two can never disagree about
     * whether a code is outstanding. distinctUntilChanged matters: serverState changes on every
     * connect/disconnect too, and re-showing an identical prompt would restart its animation.
     */
    private fun observePendingOtp() {
        scope.launch {
            _serverState
                .map { it.pendingOtp }
                .distinctUntilChanged()
                .collect { otp ->
                    if (otp != null) {
                        ConnectionPromptHost.show(otp, onRememberChange = ::setRememberDevice)
                    } else {
                        ConnectionPromptHost.dismiss()
                    }
                }
        }
    }

    private inner class DevToolsConnection(
        private val socket: DevToolsSocket,
    ) {
        private val connectionScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

        // Two channels, not one. Control messages (AUTH_*, INITIAL_STATE, snapshots) are
        // unbounded and must never be dropped: the desktop client only leaves its
        // AwaitingAuth state on AUTH_SUCCESS, so a burst of >1024 stream messages evicting
        // that one frame left the client hung on the OTP screen with no error at all.
        private val control = Channel<DevToolsMessage>(Channel.UNLIMITED)

        // Stream traffic is lossy by design — a slow client should shed telemetry, not stall
        // the capture path.
        private val stream = Channel<DevToolsMessage>(
            capacity = DevToolsDefaults.STREAM_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        private val eventAdapter = DevToolsStreamAdapter { event: TelemetryEvent -> event.time }
        private val apiLogSignatures = linkedMapOf<String, Int>()
        private val otp = (1000..9999).random().toString()
        private var isAuthenticated = false

        // Guards close() against re-entry: both loops call it from their finally blocks.
        @Volatile
        private var closed = false

        fun start() {
            // Note: pendingOtp is deliberately NOT set here. A client that already holds a trust
            // token authenticates without any user involvement, and publishing the code up front
            // would flash the prompt on screen for every reconnect — the exact noise
            // trust-on-first-use exists to remove. The code is revealed in handleAuthResponse
            // once we know no valid token was offered.
            control.trySend(AuthChallengeMessage(nextSequence()))
            connectionScope.launch { revealOtpIfClientStaysSilent() }
            // Exactly one writer coroutine: concurrent writers would interleave bytes from
            // two frames on the same socket and corrupt the stream.
            connectionScope.launch { writerLoop() }
            connectionScope.launch { readerLoop() }
        }

        fun close() {
            if (closed) return
            closed = true
            control.close()
            stream.close()
            socket.close()
            if (activeConnection === this) {
                activeConnection = null
                _serverState.value = _serverState.value.copy(hasClient = false, pendingOtp = null)
            }
            // Cancelled last: cancelling the scope kills whichever loop called us, so the
            // bookkeeping above has to be done first.
            connectionScope.coroutineContext.cancel()
        }

        private suspend fun writerLoop() {
            try {
                while (true) {
                    // Control first. Draining pending control messages before touching stream
                    // traffic means a telemetry backlog can never delay AUTH_SUCCESS or
                    // INITIAL_STATE, which the client blocks on.
                    val message = control.tryReceive().getOrNull()
                        ?: select {
                            control.onReceiveCatching { it.getOrNull() }
                            stream.onReceiveCatching { it.getOrNull() }
                        }
                        ?: break
                    socket.write(DevToolsProtocol.encodeEnvelope(message))
                }
            } finally {
                // A dead writer must tear down the whole connection. connectionScope is a
                // SupervisorJob, so without this a failed write (half-closed peer — laptop
                // asleep, adb forward removed) killed only this loop, leaving readerLoop
                // parked in readFully forever with no socket timeout to rescue it: a session
                // reporting "Connected" with zero throughput.
                close()
            }
        }

        private suspend fun readerLoop() {
            try {
                while (true) {
                    // null means EOF, bad magic, version mismatch, over-size payload, or
                    // undecodable JSON — all terminal for this connection.
                    val message = DevToolsProtocol.readEnvelope(socket) ?: break
                    if (!isAuthenticated) {
                        if (message is AuthResponseMessage) handleAuthResponse(message)
                        continue
                    }
                    when (message) {
                        is RequestInitialStateMessage -> sendInitialState()
                        is RequestClearMessage -> handleClear(message)
                        is RequestDatabaseSchemaMessage -> handleDatabaseSchemaRequest(message.databaseName)
                        is RequestDatabaseTableMessage -> handleDatabaseRequest(
                            message.databaseName,
                            message.tableName,
                            message.limit,
                        )
                        is RequestPrefValueMessage -> handlePreferenceRequest(message.key)
                        is RequestReplayTraceMessage -> handleReplayRequest(message)
                        // Includes UnknownMessage from a newer peer: ignore, don't disconnect.
                        else -> Unit
                    }
                }
            } finally {
                // In a finally, not after the loop: previously any throw from the read path
                // escaped past close(), so activeConnection stayed non-null, hasClient stayed
                // true, and the socket plus both stream collectors leaked for the process
                // lifetime while the in-app UI still claimed a client was attached.
                close()
            }
        }

        /**
         * A client that predates trust-on-first-use never sends the token probe, so it would sit
         * waiting for a code the device never displayed — an opaque hang with no error on either
         * side. Reveal the OTP anyway after a short grace period.
         */
        private suspend fun revealOtpIfClientStaysSilent() {
            delay(DevToolsDefaults.OTP_REVEAL_GRACE_MILLIS)
            if (!isAuthenticated && !closed && _serverState.value.pendingOtp == null) {
                _serverState.value = _serverState.value.copy(pendingOtp = otp)
                send(AuthOtpRequiredMessage(nextSequence()))
            }
        }

        private fun handleAuthResponse(message: AuthResponseMessage) {
            val token = message.token
            when {
                // Known desktop: authenticate silently, show nothing, issue nothing new.
                token != null && trustStore.isTrusted(token) -> {
                    authenticate(issuedToken = null)
                }

                // The probe a client sends when it holds no usable token. Not a failed attempt,
                // so it must not close the connection — it is the signal to show the code and
                // wait for the user to type it.
                message.otp.isEmpty() -> {
                    _serverState.value = _serverState.value.copy(pendingOtp = otp)
                    send(AuthOtpRequiredMessage(nextSequence()))
                }

                message.otp == this.otp -> {
                    // Mint a token only if the user asked for it. Without consent this stays a
                    // one-off session and the code is required again next time.
                    authenticate(
                        issuedToken = trustStore.tokenForAcceptedOtp(
                            rememberDevice = _serverState.value.rememberDevice,
                        ),
                    )
                }

                else -> {
                    send(AuthFailureMessage(nextSequence(), "Invalid OTP"))
                    close()
                }
            }
        }

        private fun authenticate(issuedToken: String?) {
            isAuthenticated = true
            _serverState.value = _serverState.value.copy(pendingOtp = null)
            send(AuthSuccessMessage(nextSequence(), token = issuedToken))
            connectionScope.launch { streamEvents() }
            connectionScope.launch { streamApiLogs() }
            connectionScope.launch { sendInitialState() }
        }

        /**
         * Deletes captured data at the source, then re-sends the snapshot.
         *
         * The re-send matters: the desktop clears its own stores optimistically, and without a
         * fresh snapshot the two sides would disagree until the next reconnect.
         */
        private suspend fun handleClear(message: RequestClearMessage) {
            if (message.traces) database.traceDao().clearAll()
            if (message.events) database.telemetryDao().clearAll()
            sendInitialState()
        }

        /**
         * Re-sends a captured request through the host app's client and reports the outcome.
         *
         * Launched on [connectionScope] rather than awaited inline: a replay is a real network call
         * that can sit for the app's full read timeout, and awaiting it in [readerLoop] would stall
         * every other command behind it — the desktop could not so much as refresh a table until it
         * returned.
         */
        private fun handleReplayRequest(message: RequestReplayTraceMessage) {
            connectionScope.launch {
                val request = message.request
                val outcome = TraceReplayRegistry.replay(request)
                send(
                    when (outcome) {
                        is ReplayOutcome.Sent -> ReplayResultMessage(
                            sequence = nextSequence(),
                            sourceTraceId = request.sourceTraceId,
                            sent = true,
                            traceId = outcome.traceId,
                        )

                        is ReplayOutcome.Failed -> ReplayResultMessage(
                            sequence = nextSequence(),
                            sourceTraceId = request.sourceTraceId,
                            sent = false,
                            error = outcome.reason,
                        )
                    },
                )
            }
        }

        private suspend fun sendInitialState() {
            val events = database.telemetryDao()
                .getLatest(DevToolsDefaults.EVENT_SNAPSHOT_LIMIT)
            val apiLogs = database.traceDao()
                .getLatest(DevToolsDefaults.API_LOG_SNAPSHOT_LIMIT)
            val databases = databaseInspector.listDatabases()
            val selectedDatabase = databases.firstOrNull()?.name
            defaultDatabaseName = selectedDatabase
            val schema = if (selectedDatabase == null) {
                DatabaseSchemaSnapshot(
                    databaseName = null,
                    tables = emptyList(),
                    schemas = emptyList(),
                )
            } else {
                databaseInspector.loadSchema(selectedDatabase)
            }
            val preferenceKeys = preferencesInspector.getAllKeys()
            val payload = InitialStatePayload(
                events = events,
                apiLogs = apiLogs,
                databaseSchema = schema,
                databases = databases,
                selectedDatabase = selectedDatabase,
                preferenceKeys = preferenceKeys,
                buildInfo = Alohomora.config?.toBuildInfoPayload(),
                chronicle = Alohomora.config?.commits?.map { it.toChronicleCommitPayload() }.orEmpty(),
                replaySupported = TraceReplayRegistry.isSupported,
            )
            eventAdapter.seed(events)
            seedApiLogSignatures(apiLogs)
            send(InitialStateMessage(nextSequence(), payload))
        }

        private suspend fun streamEvents() {
            telemetryRepository.list("", 0, DevToolsDefaults.EVENT_SNAPSHOT_LIMIT).collect { events ->
                val newItems = eventAdapter.filterNew(events)
                newItems.forEach { item ->
                    sendStream(StreamEventMessage(nextSequence(), item))
                }
            }
        }

        private suspend fun streamApiLogs() {
            database.traceDao().observeLatest(DevToolsDefaults.API_LOG_SNAPSHOT_LIMIT).collect { logs ->
                val changedItems = changedApiLogs(logs)
                changedItems.forEach { item ->
                    sendStream(StreamApiLogMessage(nextSequence(), item))
                }
            }
        }

        private fun seedApiLogSignatures(logs: List<TraceEntry>) {
            apiLogSignatures.clear()
            logs.forEach { log ->
                apiLogSignatures[log.id] = log.streamSignature()
            }
        }

        private fun changedApiLogs(logs: List<TraceEntry>): List<TraceEntry> {
            val visibleIds = logs.mapTo(mutableSetOf()) { it.id }
            apiLogSignatures.keys.retainAll(visibleIds)

            val changed = mutableListOf<TraceEntry>()
            logs.asReversed().forEach { log ->
                val signature = log.streamSignature()
                if (apiLogSignatures[log.id] != signature) {
                    apiLogSignatures[log.id] = signature
                    changed += log
                }
            }
            return changed
        }

        private fun TraceEntry.streamSignature(): Int =
            listOf(
                status,
                message,
                method,
                scheme,
                host,
                path,
                query,
                requestBody,
                responseBody,
                duration,
                requestHeaders,
                requestContentType,
                responseContentType,
                responseHeaders,
                requestSize,
                responseSize,
                time,
            ).hashCode()

        private suspend fun handleDatabaseRequest(databaseName: String?, tableName: String, limit: Int) {
            val resolvedName = databaseName ?: defaultDatabaseName ?: return
            val tableSnapshot = databaseInspector.loadTable(resolvedName, tableName, limit)
            send(DatabaseSnapshotMessage(
                nextSequence(),
                DatabaseSnapshotPayload(databaseName = resolvedName, table = tableSnapshot),
            ))
        }

        private suspend fun handleDatabaseSchemaRequest(databaseName: String) {
            defaultDatabaseName = databaseName
            val schema = databaseInspector.loadSchema(databaseName)
            send(DatabaseSnapshotMessage(
                nextSequence(),
                DatabaseSnapshotPayload(databaseName = databaseName, schema = schema),
            ))
        }

        private suspend fun handlePreferenceRequest(key: String) {
            val value = preferencesInspector.getValue(key)
            send(PrefsSnapshotMessage(
                nextSequence(),
                PrefsSnapshotPayload(values = mapOf(key to value)),
            ))
        }

        /** Queues a control message. Never dropped — [control] is unbounded. */
        private fun send(message: DevToolsMessage) {
            control.trySend(message)
        }

        /**
         * Queues lossy stream traffic.
         *
         * Note there is deliberately no "dropped" log here: [stream] uses DROP_OLDEST, and
         * `trySend` on a DROP_OLDEST channel never reports failure for a full buffer (only for
         * a closed one). The old failure branch could never fire for the case it was written
         * for, so it advertised drop detection that did not exist.
         */
        private fun sendStream(message: DevToolsMessage) {
            stream.trySend(message)
        }
    }
}
