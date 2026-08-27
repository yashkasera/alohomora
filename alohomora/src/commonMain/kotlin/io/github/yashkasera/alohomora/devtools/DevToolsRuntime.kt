package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.AuthChallengeMessage
import io.github.yashkasera.alohomora.common.AuthFailureMessage
import io.github.yashkasera.alohomora.common.AuthOtpRequiredMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.CacheSnapshotMessage
import io.github.yashkasera.alohomora.common.CacheSnapshotPayload
import io.github.yashkasera.alohomora.common.CustomActionResultMessage
import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DatabaseSnapshotMessage
import io.github.yashkasera.alohomora.common.DatabaseSnapshotPayload
import io.github.yashkasera.alohomora.common.DevToolsHeartbeat
import io.github.yashkasera.alohomora.common.DevToolsLiveness
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.DeviceErrorMessage
import io.github.yashkasera.alohomora.common.EnvelopeRead
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlagsSnapshotMessage
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.PingMessage
import io.github.yashkasera.alohomora.common.PluginDataUpdateResultMessage
import io.github.yashkasera.alohomora.common.ReplayResultMessage
import io.github.yashkasera.alohomora.common.RequestCacheDeleteMessage
import io.github.yashkasera.alohomora.common.RequestCacheRefreshMessage
import io.github.yashkasera.alohomora.common.RequestCacheUpdateMessage
import io.github.yashkasera.alohomora.common.RequestCacheValueMessage
import io.github.yashkasera.alohomora.common.RequestClearMessage
import io.github.yashkasera.alohomora.common.RequestCustomActionMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseTableMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseUpdateMessage
import io.github.yashkasera.alohomora.common.RequestInitialStateMessage
import io.github.yashkasera.alohomora.common.RequestPluginDataUpdateMessage
import io.github.yashkasera.alohomora.common.RequestReplayTraceMessage
import io.github.yashkasera.alohomora.common.RequestTraceSpansMessage
import io.github.yashkasera.alohomora.common.ServerShuttingDownMessage
import io.github.yashkasera.alohomora.common.SetMockRulesMessage
import io.github.yashkasera.alohomora.common.SetThrottleProfileMessage
import io.github.yashkasera.alohomora.common.SetVpnThrottleMessage
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.StreamErrorMessage
import io.github.yashkasera.alohomora.common.StreamEventMessage
import io.github.yashkasera.alohomora.common.StreamPluginDataMessage
import io.github.yashkasera.alohomora.common.StreamSpanMessage
import io.github.yashkasera.alohomora.common.StreamTrafficMessage
import io.github.yashkasera.alohomora.common.TraceSpansSnapshotMessage
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.VpnStateMessage
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults.ERROR_SNAPSHOT_LIMIT
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults.MAX_TABLE_RELOAD_LIMIT
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults.TRAFFIC_SNAPSHOT_LIMIT
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.replay.ReplayOutcome
import io.github.yashkasera.alohomora.replay.TrafficReplayRegistry
import io.github.yashkasera.alohomora.trace.SpanCaptureRegistry
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select

internal object DevToolsDefaults {
    const val DEFAULT_PORT: Int = 53999
    const val EVENT_SNAPSHOT_LIMIT: Int = 500
    const val TRAFFIC_SNAPSHOT_LIMIT: Int = 200

    /**
     * Deliberately far below the event and traffic limits: every error carries a full stack trace,
     * so a row here is an order of magnitude heavier than either of those.
     */
    const val ERROR_SNAPSHOT_LIMIT: Int = 100

    /**
     * Above the event limit and far above [ERROR_SNAPSHOT_LIMIT], because the unit differs.
     *
     * A span row is cheap — two ids, two longs, a small attribute object — but a *trace* is 10-25 of
     * them, so 1000 spans is only ~40-100 traces: the same order as [TRAFFIC_SNAPSHOT_LIMIT]'s 200
     * requests, measured in the unit a developer actually thinks in.
     *
     * Frame budget: ~1000 x 600 B is ~600 KB, comfortably inside `DevToolsProtocol.MAX_PAYLOAD_BYTES`
     * alongside the event and traffic snapshots — but that arithmetic only holds because
     * `SPAN_ATTRIBUTE_VALUE_MAX_CHARS` bounds attribute values, which no tracer does on its own.
     */
    const val SPAN_SNAPSHOT_LIMIT: Int = 1000
    const val STREAM_BUFFER_CAPACITY: Int = 1024
    const val MAX_TABLE_RELOAD_LIMIT: Int = 200

    /**
     * How long to wait for a client's token probe before displaying the OTP regardless.
     * Long enough that a token-bearing client never flashes the prompt on a local socket, short
     * enough that a client which never probes is not left waiting.
     */
    const val OTP_REVEAL_GRACE_MILLIS: Long = 400
}

@OptIn(ExperimentalAtomicApi::class)
internal class DevToolsRuntime(
    private val eventsRepository: EventsRepository,
    private val database: AlohomoraDb,
    private val cacheInspector: DevToolsCacheInspector,
    private val server: DevToolsTcpServer,
    appDatabaseProvider: DevToolsAppDatabaseProvider,
    private val trustStore: DevToolsTrustStore,
    private val featureFlagStore: FeatureFlagStore,
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

    @Volatile
    private var isObservingServerActive = false
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
        if (!isObservingServerActive) {
            isObservingServerActive = true
            observeServerActive()
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
        ServerActiveNotificationHost.dismiss()
        listenPort = null
        activeConnection?.shutDownGracefully()
        activeConnection = null
        server.stop()
        _serverState.value = _serverState.value.copy(
            isRunning = false,
            hasClient = false,
            port = null,
        )
    }

    private fun attachClient(socket: DevToolsSocket) {
        // Reject rather than evict. The previous behaviour was last-writer-wins, which let
        // any unauthenticated peer repeatedly kick the developer's live session off (and
        // reset the displayed OTP each time) simply by reconnecting.
        //
        // Rejecting is only safe because the incumbent is reapable: an attached client that dies
        // without a FIN used to hold this slot until the app was restarted. See
        // DevToolsConnection.heartbeatLoop.
        //
        // One peer still escapes that: one which connects and then sends nothing at all, since the
        // heartbeat is armed by the client's probe. A bound on pre-probe silence was considered and
        // rejected — a client predating trust-on-first-use deliberately stays silent until the user
        // types the OTP, so any bound short enough to be useful would drop it mid-pairing. Current
        // clients send the probe unconditionally within milliseconds, which is also what the
        // OTP_REVEAL_GRACE_MILLIS window already assumes.
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

    private fun observeServerActive() {
        scope.launch {
            _serverState
                .map { it.isRunning to it.hasClient }
                .distinctUntilChanged()
                .collect { (running, hasClient) ->
                    if (running) {
                        val port = listenPort ?: DevToolsDefaults.DEFAULT_PORT
                        ServerActiveNotificationHost.show(port, hasClient)
                    } else {
                        ServerActiveNotificationHost.dismiss()
                    }
                }
        }
    }

    private inner class DevToolsConnection(
        private val socket: DevToolsSocket,
    ) {
        private val connectionScope =
            CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)

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
        private val eventAdapter = DevToolsStreamAdapter { event: Event -> event.time }

        // Keyed on `id`, not `time`: ids are monotonic, so two errors recorded inside the same
        // millisecond are still ordered and neither is silently dropped as "not newer".
        private val errorAdapter = DevToolsStreamAdapter { error: Error -> error.id }

        // Keyed on the rowid, not on either timestamp, and this one is not interchangeable: a parent
        // span ends *after* its children, a later-started sibling can end before an earlier one, and
        // two spans can share an end nanos. An end-ordered key would therefore both emit spans out of
        // order and silently drop the ones that tie. Rowids are monotonic in insert order.
        private val spanAdapter = DevToolsStreamAdapter { span: Span -> span.id }
        private val trafficSignatures = linkedMapOf<String, Int>()
        private val otp = (1000..9999).random().toString()
        private var isAuthenticated = false
        private val liveness = DevToolsLiveness()

        // Guards startHeartbeat against re-entry: the client sends an AuthResponseMessage twice
        // on a first pairing (the token probe, then the code the user typed).
        @Volatile
        private var heartbeatStarted = false

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
                    // A graceful-shutdown frame is the last thing this connection ever sends. Break
                    // only *after* the write so the frame is on the wire before finally→close()
                    // tears the socket down — the whole point is that the desktop reads it and stops
                    // reconnecting. Done here, in the sole writer, rather than by close() racing the
                    // write: two writers on one socket interleave bytes and corrupt the stream.
                    if (message is ServerShuttingDownMessage) break
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A half-closed peer (laptop asleep, adb forward removed) surfaces here. Worth a
                // line: the session otherwise just stops with no explanation on either side.
                println("[Alohomora] DevTools write failed, closing session: $e")
            } finally {
                // A dead writer must tear down the whole connection. connectionScope is a
                // SupervisorJob, so without this a failed write (half-closed peer — laptop
                // asleep, adb forward removed) killed only this loop, leaving readerLoop
                // parked in readFully forever with no socket timeout to rescue it: a session
                // reporting "Connected" with zero throughput.
                println("[Alohomora] Device: Connection closing")
                close()
            }
        }

        private suspend fun readerLoop() {
            try {
                while (true) {
                    // null means EOF, bad magic, version mismatch, over-size payload, or
                    // undecodable JSON — all terminal for this connection.
                    val message = when (val read = DevToolsProtocol.readEnvelope(socket)) {
                        is EnvelopeRead.Message -> read.message
                        is EnvelopeRead.EndOfStream -> break
                        is EnvelopeRead.Malformed -> {
                            println("[Alohomora] DevTools dropping connection: ${read.reason}")
                            break
                        }
                        // Reported on the device too, not just to the desktop. The developer is
                        // holding the phone, and "your desktop app is too old" is only actionable
                        // if someone says it.
                        is EnvelopeRead.VersionMismatch -> {
                            val reason = "Desktop speaks DevTools protocol v${read.peerVersion}, " +
                                "this app speaks v${read.localVersion}. Update the older of the two."
                            println("[Alohomora] $reason")
                            _serverState.value = _serverState.value.copy(lastError = reason)
                            break
                        }
                    }
                    liveness.recordSignOfLife()
                    if (!isAuthenticated) {
                        if (message is AuthResponseMessage) handleAuthResponse(message)
                        continue
                    }
                    try {
                        when (message) {
                            is RequestInitialStateMessage -> sendInitialState()
                            is RequestClearMessage -> handleClear(message)
                            is RequestDatabaseSchemaMessage -> handleDatabaseSchemaRequest(message.databaseName)
                            is RequestDatabaseTableMessage -> handleDatabaseRequest(
                                message.databaseName,
                                message.tableName,
                                message.limit,
                            )

                            is RequestDatabaseUpdateMessage -> handleDatabaseUpdate(message)
                            is RequestCacheValueMessage -> handleCacheRequest(message.key)
                            is RequestCacheUpdateMessage -> handleCacheUpdate(message)
                            is RequestCacheDeleteMessage -> handleCacheDelete(message)
                            is RequestCacheRefreshMessage -> handleCacheRefresh()
                            is RequestReplayTraceMessage -> handleReplayRequest(message)
                            is RequestTraceSpansMessage -> handleTraceSpansRequest(message.traceId)
                            is SetThrottleProfileMessage -> NetworkRuleEngine.setThrottle(message.profile)
                            is SetMockRulesMessage -> NetworkRuleEngine.setMockRules(message.rules)
                            is SetVpnThrottleMessage -> handleVpnThrottle(message)
                            is RequestCustomActionMessage -> handleCustomAction(message)
                            is RequestPluginDataUpdateMessage -> handlePluginDataUpdate(message)
                            // Includes UnknownMessage from a newer peer: ignore, don't disconnect.
                            else -> Unit
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val request = message::class.simpleName ?: "unknown request"
                        println("[Alohomora] DevTools handler failed for $request: $e")
                        // Tell the desktop. It is waiting on a reply that is no longer coming, and
                        // without this it waits forever with nothing to show the developer.
                        send(
                            DeviceErrorMessage(
                                sequence = nextSequence(),
                                request = request,
                                message = e.message ?: e.toString(),
                            ),
                        )
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
            delay(DevToolsDefaults.OTP_REVEAL_GRACE_MILLIS.milliseconds)
            if (!isAuthenticated && !closed && _serverState.value.pendingOtp == null) {
                _serverState.value = _serverState.value.copy(pendingOtp = otp)
                send(AuthOtpRequiredMessage(nextSequence()))
            }
        }

        private fun handleAuthResponse(message: AuthResponseMessage) {
            // Started from the probe rather than from authenticate(): the connection slot is held
            // from the moment the socket is accepted, so a client that dies while its code is on
            // screen wedges the device exactly as thoroughly as an authenticated one.
            if (message.heartbeatSupported) startHeartbeat()
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

        /** Idempotent: only the first capable [AuthResponseMessage] starts the loop. */
        private fun startHeartbeat() {
            if (heartbeatStarted) return
            heartbeatStarted = true
            connectionScope.launch { heartbeatLoop() }
        }

        /**
         * Pings the client and reclaims the connection once it stops answering.
         *
         * This is the only thing that can free the slot when a peer dies without a FIN — the case
         * an `adb forward` produces routinely, because the device's socket is to the on-device adb
         * daemon and stays healthy after the host process is gone. Neither existing loop notices:
         * [readerLoop] is parked in a read with no timeout (deliberately — see [DevToolsLiveness]),
         * and [writerLoop] only discovers a dead socket on a write it never makes while the app is
         * idle. So [attachClient] went on rejecting every later client until the app was restarted.
         *
         * Reaping is by total silence, not by unanswered ping count: a session streaming traffic is
         * already proving its liveness, and only an idle one needs provoking.
         */
        private suspend fun heartbeatLoop() {
            while (true) {
                delay(DevToolsHeartbeat.PING_INTERVAL_MILLIS.milliseconds)
                // Checked before sending, so the ping about to go out is never counted as one the
                // client failed to answer.
                if (liveness.isPeerSilent()) {
                    println(
                        "[Alohomora] DevTools client silent for ${liveness.silentForMillis()}ms; " +
                            "reclaiming the connection.",
                    )
                    close()
                    return
                }
                // Queued, not written here. If the socket is already unwritable the writer is stuck
                // and these accumulate in the unbounded control channel — bounded in practice by
                // the handful of intervals it takes the check above to fire and close() to run.
                send(PingMessage(nextSequence()))
            }
        }

        private fun authenticate(issuedToken: String?) {
            isAuthenticated = true
            _serverState.value = _serverState.value.copy(pendingOtp = null)
            send(AuthSuccessMessage(nextSequence(), token = issuedToken))
            connectionScope.launch { streamEvents() }
            connectionScope.launch { streamTraffic() }
            connectionScope.launch { streamErrors() }
            connectionScope.launch { streamSpans() }
            connectionScope.launch { streamFeatureFlags() }
            connectionScope.launch { streamPluginData() }
            connectionScope.launch { observeVpnState() }
            connectionScope.launch { sendInitialState() }
        }

        /**
         * Deletes captured data at the source, then re-sends the snapshot.
         *
         * The re-send matters: the desktop clears its own stores optimistically, and without a
         * fresh snapshot the two sides would disagree until the next reconnect.
         */
        private suspend fun handleClear(message: RequestClearMessage) {
            // `traces` means *traffic* — a misnomer predating the vocabulary rule, kept for interop.
            // Spans have their own flag; see RequestClearMessage.spans.
            if (message.traces) database.trafficDao().clearAll()
            if (message.events) database.eventDao().clearAll()
            if (message.errors) database.errorDao().clearAll()
            if (message.spans) database.spanDao().clearAll()
            sendInitialState()
        }

        /**
         * Answers [RequestTraceSpansMessage] with every span of one trace.
         *
         * Exists because the snapshot truncates by rowid, which cuts a large trace's
         * earliest-finishing leaves while keeping its root — so the desktop can hold what looks like a
         * complete waterfall and is not. This lets it ask for the rest.
         */
        private suspend fun handleTraceSpansRequest(traceId: String) {
            val spans = database.spanDao().getTrace(traceId)
            send(TraceSpansSnapshotMessage(nextSequence(), traceId, spans))
        }

        /**
         * Re-sends a captured request through the host app's client and reports the outcome.
         *
         * Launched on [connectionScope] rather than awaited inline: a replay is a real network call
         * that can sit for the app's full read timeout, and awaiting it in [readerLoop] would stall
         * every other command behind it — the desktop could not so much as refresh a table until it
         * returned.
         */
        private fun handleCustomAction(message: RequestCustomActionMessage) {
            connectionScope.launch {
                val result = try {
                    val output = DevToolsActionRegistry.execute(message.actionId, message.params)
                    CustomActionResultMessage(
                        sequence = nextSequence(),
                        actionId = message.actionId,
                        success = true,
                        result = output,
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    CustomActionResultMessage(
                        sequence = nextSequence(),
                        actionId = message.actionId,
                        success = false,
                        error = e.message ?: e.toString(),
                    )
                }
                send(result)
            }
        }

        private fun handlePluginDataUpdate(message: RequestPluginDataUpdateMessage) {
            connectionScope.launch {
                val field = DevToolsPluginDataRegistry.getField(message.pluginId, message.key)
                if (field == null) {
                    send(
                        PluginDataUpdateResultMessage(
                            sequence = nextSequence(),
                            pluginId = message.pluginId,
                            key = message.key,
                            success = false,
                            error = "No data field '${message.key}' on plugin '${message.pluginId}'",
                        ),
                    )
                    return@launch
                }
                if (field.readOnly || field.onUpdate == null) {
                    send(
                        PluginDataUpdateResultMessage(
                            sequence = nextSequence(),
                            pluginId = message.pluginId,
                            key = message.key,
                            success = false,
                            error = "Field '${message.key}' is read-only",
                        ),
                    )
                    return@launch
                }
                try {
                    field.onUpdate.onUpdate(message.value)
                    DevToolsPluginDataRegistry.notifyChanged(message.pluginId)
                    send(
                        PluginDataUpdateResultMessage(
                            sequence = nextSequence(),
                            pluginId = message.pluginId,
                            key = message.key,
                            success = true,
                        ),
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    send(
                        PluginDataUpdateResultMessage(
                            sequence = nextSequence(),
                            pluginId = message.pluginId,
                            key = message.key,
                            success = false,
                            error = e.message ?: e.toString(),
                        ),
                    )
                }
            }
        }

        private suspend fun streamPluginData() {
            DevToolsPluginDataRegistry.changes.collect { pluginId ->
                val snapshot = DevToolsPluginDataRegistry.getSnapshot(pluginId) ?: return@collect
                sendStream(StreamPluginDataMessage(nextSequence(), snapshot))
            }
        }

        private fun handleReplayRequest(message: RequestReplayTraceMessage) {
            connectionScope.launch {
                val request = message.request
                val outcome = TrafficReplayRegistry.replay(request)
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
            val events = database.eventDao()
                .getLatest(DevToolsDefaults.EVENT_SNAPSHOT_LIMIT)
            val traffic = database.trafficDao()
                .getLatest(TRAFFIC_SNAPSHOT_LIMIT)
            val errors = database.errorDao()
                .getLatest(ERROR_SNAPSHOT_LIMIT)
            val spans = database.spanDao()
                .getLatest(DevToolsDefaults.SPAN_SNAPSHOT_LIMIT)
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
            val cacheKeys = cacheInspector.getAllKeys()
            val cacheStores = cacheInspector.getStores()
            val payload = InitialStatePayload(
                events = events,
                traffic = traffic,
                errors = errors,
                spans = spans,
                databaseSchema = schema,
                databases = databases,
                selectedDatabase = selectedDatabase,
                cacheKeys = cacheKeys,
                cacheStores = cacheStores,
                buildMetadata = AlohomoraImpl.config?.toBuildMetadataPayload(),
                gitHistory = AlohomoraImpl.config?.commits?.map { it.toGitHistoryPayload() }.orEmpty(),
                replaySupported = TrafficReplayRegistry.isSupported,
                spanCaptureSupported = SpanCaptureRegistry.isSupported,
                networkRulesSupported = true,
                vpnThrottleSupported = isVpnThrottleSupported,
                vpnThrottleState = vpnThrottleStateFlow().value,
                vpnThrottleActiveProfile = vpnThrottleActiveProfile(),
                featureFlags = featureFlagStore.getAll(),
                actions = DevToolsActionRegistry.getDescriptors(),
                pluginData = DevToolsPluginDataRegistry.getSnapshots(),
            )
            eventAdapter.seed(events)
            errorAdapter.seed(errors)
            spanAdapter.seed(spans)
            seedTrafficSignatures(traffic)
            send(InitialStateMessage(nextSequence(), payload))
        }

        private suspend fun streamEvents() {
            eventsRepository.list("", 0, DevToolsDefaults.EVENT_SNAPSHOT_LIMIT).collect { events ->
                val newItems = eventAdapter.filterNew(events)
                newItems.forEach { item ->
                    sendStream(StreamEventMessage(nextSequence(), item))
                }
            }
        }

        /**
         * Streams straight off the DAO rather than through `ErrorRepository.list`, which projects
         * `stackTrace` away for the mobile list — a desktop row without its trace would be useless
         * and there is no follow-up request to fetch one.
         */
        private suspend fun streamErrors() {
            database.errorDao().observeLatest(ERROR_SNAPSHOT_LIMIT)
                .collect { errors ->
                    errorAdapter.filterNew(errors).forEach { item ->
                        sendStream(StreamErrorMessage(nextSequence(), item))
                    }
                }
        }

        /**
         * Streams straight off the DAO rather than through `SpanRepository`, for the same reason as
         * [streamErrors]: the desktop needs the whole row, attributes and events included, and has no
         * follow-up request for an individual span.
         */
        private suspend fun streamSpans() {
            database.spanDao().observeLatest(DevToolsDefaults.SPAN_SNAPSHOT_LIMIT)
                .collect { spans ->
                    spanAdapter.filterNew(spans).forEach { item ->
                        sendStream(StreamSpanMessage(nextSequence(), item))
                    }
                }
        }

        private suspend fun streamTraffic() {
            database.trafficDao().observeLatest(TRAFFIC_SNAPSHOT_LIMIT)
                .collect { logs ->
                    val changedItems = changedTraffic(logs)
                    changedItems.forEach { item ->
                        sendStream(StreamTrafficMessage(nextSequence(), item))
                    }
                }
        }

        private suspend fun streamFeatureFlags() {
            featureFlagStore.flags.collect { flags ->
                sendStream(FeatureFlagsSnapshotMessage(nextSequence(), flags.values.toList()))
            }
        }

        private fun handleVpnThrottle(message: SetVpnThrottleMessage) {
            if (message.enabled) {
                vpnThrottleEnable(message.profile)
            } else {
                vpnThrottleDisable()
            }
        }

        private suspend fun observeVpnState() {
            vpnThrottleStateFlow().collect { state ->
                send(
                    VpnStateMessage(
                        sequence = nextSequence(),
                        state = state,
                        activeProfile = vpnThrottleActiveProfile(),
                    ),
                )
            }
        }

        private fun seedTrafficSignatures(logs: List<TrafficEntry>) {
            trafficSignatures.clear()
            logs.forEach { log ->
                trafficSignatures[log.id] = log.streamSignature()
            }
        }

        private fun changedTraffic(logs: List<TrafficEntry>): List<TrafficEntry> {
            val visibleIds = logs.mapTo(mutableSetOf()) { it.id }
            trafficSignatures.keys.retainAll(visibleIds)

            val changed = mutableListOf<TrafficEntry>()
            logs.asReversed().forEach { log ->
                val signature = log.streamSignature()
                if (trafficSignatures[log.id] != signature) {
                    trafficSignatures[log.id] = signature
                    changed += log
                }
            }
            return changed
        }

        private fun TrafficEntry.streamSignature(): Int =
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

        private fun handleDatabaseRequest(
            databaseName: String?,
            tableName: String,
            limit: Int,
        ) {
            val resolvedName = databaseName ?: defaultDatabaseName ?: return
            val tableSnapshot = databaseInspector.loadTable(resolvedName, tableName, limit)
            send(
                DatabaseSnapshotMessage(
                    nextSequence(),
                    DatabaseSnapshotPayload(databaseName = resolvedName, table = tableSnapshot),
                ),
            )
        }

        private fun handleDatabaseSchemaRequest(databaseName: String) {
            defaultDatabaseName = databaseName
            val schema = databaseInspector.loadSchema(databaseName)
            send(
                DatabaseSnapshotMessage(
                    nextSequence(),
                    DatabaseSnapshotPayload(databaseName = databaseName, schema = schema),
                ),
            )
        }

        private fun handleDatabaseUpdate(message: RequestDatabaseUpdateMessage) {
            val dbName = message.databaseName
            val success = databaseInspector.updateCell(
                databaseName = dbName,
                tableName = message.tableName,
                primaryKeys = message.primaryKeys,
                columnName = message.columnName,
                newValue = message.newValue,
            )
            if (success) {
                handleDatabaseRequest(dbName, message.tableName, MAX_TABLE_RELOAD_LIMIT)
            }
        }

        private suspend fun handleCacheRequest(key: String) {
            val value = cacheInspector.getValue(key)
            send(
                CacheSnapshotMessage(
                    nextSequence(),
                    CacheSnapshotPayload(values = mapOf(key to value)),
                ),
            )
        }

        private suspend fun handleCacheUpdate(message: RequestCacheUpdateMessage) {
            cacheInspector.updateValue(
                storeName = message.storeName,
                key = message.key,
                newValue = message.newValue,
                type = message.valueType,
            )
            sendCacheSnapshot()
        }

        private suspend fun handleCacheDelete(message: RequestCacheDeleteMessage) {
            cacheInspector.deleteValue(
                storeName = message.storeName,
                key = message.key,
            )
            sendCacheSnapshot()
        }

        private suspend fun handleCacheRefresh() {
            sendCacheSnapshot()
        }

        private suspend fun sendCacheSnapshot() {
            val stores = cacheInspector.refreshStores()
            val keys = stores.flatMap { s -> s.entries.map { it.key } }
            val values = stores.flatMap { s -> s.entries.map { it.key to it.value } }.toMap()
            send(
                CacheSnapshotMessage(
                    nextSequence(),
                    CacheSnapshotPayload(
                        keys = keys,
                        values = values,
                        stores = stores,
                    ),
                ),
            )
        }

        /** Queues a control message. Never dropped — [control] is unbounded. */
        private fun send(message: DevToolsMessage) {
            control.trySend(message)
        }

        /**
         * Tells the client the server is stopping on purpose, then lets the writer close the socket.
         *
         * Queued like any other control frame rather than written inline: the writer is the only
         * coroutine allowed to touch the socket, and it closes the connection the moment this frame
         * goes out (see [writerLoop]). Best-effort — if the peer is already gone the write fails and
         * the finally block closes anyway. The caller must not also call [close] first, which would
         * cancel the writer before it could send this.
         */
        fun shutDownGracefully() {
            send(ServerShuttingDownMessage(nextSequence()))
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
