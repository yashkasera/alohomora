package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.AuthFailureMessage
import io.github.yashkasera.alohomora.common.AuthOtpRequiredMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.DatabaseSnapshotMessage
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.PrefsSnapshotMessage
import io.github.yashkasera.alohomora.common.ReplayResultMessage
import io.github.yashkasera.alohomora.common.RequestClearMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseTableMessage
import io.github.yashkasera.alohomora.common.RequestInitialStateMessage
import io.github.yashkasera.alohomora.common.RequestPrefValueMessage
import io.github.yashkasera.alohomora.common.RequestReplayTraceMessage
import io.github.yashkasera.alohomora.common.StreamApiLogMessage
import io.github.yashkasera.alohomora.common.StreamEventMessage
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.data.local.ApiLogStore
import io.github.yashkasera.alohomora.desktop.data.local.BuildInfoStore
import io.github.yashkasera.alohomora.desktop.data.local.ChronicleStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.PrefsStore
import io.github.yashkasera.alohomora.desktop.data.local.ReplayStore
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.ChronicleCommit
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import io.github.yashkasera.alohomora.desktop.domain.model.ReplayState
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DevToolsRepositoryImpl(
    private val remoteDataSource: DevToolsRemoteDataSource,
    private val eventStore: EventStore,
    private val apiLogStore: ApiLogStore,
    private val databaseStore: DatabaseSnapshotStore,
    private val prefsStore: PrefsStore,
    private val buildInfoStore: BuildInfoStore,
    private val chronicleStore: ChronicleStore,
    private val replayStore: ReplayStore,
) : DevToolsRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<DevToolsConnection>(DevToolsConnection.Disconnected)
    override val connectionState: StateFlow<DevToolsConnection> = _state.asStateFlow()
    private val _currentDeviceId = MutableStateFlow<String?>(null)
    override val currentDeviceId: StateFlow<String?> = _currentDeviceId.asStateFlow()
    private val _switching = MutableStateFlow(false)
    override val switching: StateFlow<Boolean> = _switching.asStateFlow()

    override val events: StateFlow<List<TelemetryEvent>> = eventStore.events
    override val apiLogs: StateFlow<List<TraceEntry>> = apiLogStore.logs
    override val databaseSnapshot: StateFlow<DatabaseSnapshot> = databaseStore.snapshot
    override val prefsState: StateFlow<PrefsState> = prefsStore.state
    override val buildInfo: StateFlow<BuildInfo?> = buildInfoStore.buildInfo
    override val chronicle: StateFlow<List<ChronicleCommit>> = chronicleStore.commits
    override val replayState: StateFlow<ReplayState> = replayStore.state

    @Volatile
    private var connection: DevToolsSocket? = null
    private val writeMutex = Mutex()

    /**
     * The live connection coroutine, and a monotonically increasing token identifying it.
     *
     * `disconnect(); connect(...)` used to race: disconnect flipped a plain Boolean and nulled
     * the socket field without cancelling the reader, so connect immediately passed its guard
     * and launched a second reader. When the first coroutine's read finally failed, its
     * `finally` closed the **new** socket and stomped the state back to Disconnected — device
     * switching intermittently landed on a dead session. A dying job now checks [generation]
     * before touching any shared state.
     */
    private var connectionJob: Job? = null

    @Volatile
    private var generation: Int = 0

    /** Serialises connect/disconnect so only one may mutate the connection at a time. */
    private val lifecycleMutex = Mutex()

    override fun connect(target: DevToolsTarget) {
        val host = target.displayHost
        val port = target.port
        scope.launch {
            lifecycleMutex.withLock {
                cancelConnectionLocked()
                val myGeneration = ++generation
                _switching.value = true
                _state.value = DevToolsConnection.Connecting(host, port)
                connectionJob = scope.launch {
                    // Retry rather than give up. An iOS app suspended in the background stops
                    // servicing its socket, which is indistinguishable from a crash from here —
                    // and the old behaviour dropped straight to Disconnected, forcing a manual
                    // reconnect every time the user glanced at their phone. Trust-on-first-use
                    // means the reconnect needs no OTP, so a resume can be invisible.
                    var attempt = 0
                    while (isActive && generation == myGeneration) {
                        runSession(target, host, port, myGeneration)
                        if (generation != myGeneration || !isActive) return@launch

                        attempt += 1
                        _state.value = DevToolsConnection.Reconnecting(host, port, attempt)
                        // Capped backoff. The common case is a foregrounded app coming straight
                        // back, so the first retries are quick; the cap keeps a genuinely dead
                        // device from being hammered.
                        delay(reconnectDelayMillis(attempt))
                    }
                }
            }
        }
    }

    /**
     * Cancels and awaits the current connection coroutine.
     *
     * Must be called holding [lifecycleMutex]. Bumping [generation] first means the outgoing
     * job's `finally` sees itself as stale and leaves shared state alone.
     */
    private suspend fun cancelConnectionLocked() {
        generation++
        connection?.close()
        connection = null
        connectionJob?.cancelAndJoin()
        connectionJob = null
    }


    /**
     * Runs one connection attempt to completion.
     *
     * Returns when the socket closes for any reason; the caller decides whether to retry. Never
     * throws for an ordinary drop — a failed attempt is normal here, not exceptional.
     */
    private suspend fun runSession(
        target: DevToolsTarget,
        host: String,
        port: Int,
        myGeneration: Int,
    ) {
        var socket: DevToolsSocket? = null
        try {
            socket = when (target) {
                is DevToolsTarget.Tcp -> remoteDataSource.connect(target.host, target.port)
                // usbmuxd hands back a socket already tunnelled to the device port, so there is
                // no host-side forward to set up first.
                is DevToolsTarget.Usbmux ->
                    remoteDataSource.connectOverUsbmux(target.usbmuxDeviceId, target.port)
            }
            // A newer connect may have superseded us while we were connecting.
            if (generation != myGeneration) return
            connection = socket
            _state.value = DevToolsConnection.AwaitingAuth(host, port)
            _switching.value = false
            // Offer whatever token we hold for this device before the user is asked for
            // anything. Sent unconditionally — an empty probe is how the device learns it must
            // display a code, so skipping it when we have no token would leave the prompt hidden
            // until the device's grace timer fired.
            sendMessage(
                AuthResponseMessage(token = DesktopTrustPrefs.tokenFor(_currentDeviceId.value)),
            )
            remoteDataSource.processConnection(socket, ::handleMessage)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Swallowed on purpose: the caller retries, and surfacing every failed attempt as
            // Failed would flicker the UI between error and reconnecting once a second.
        } finally {
            socket?.close()
            if (generation == myGeneration) {
                connection = null
                _switching.value = false
                // The result messages these were waiting on died with the socket.
                replayStore.abandonInFlight()
            }
        }
    }


    override fun switchDevice(target: DevToolsTarget, deviceId: String?) {
        _currentDeviceId.value = deviceId
        clearAll()
        val current = _state.value
        val isSameTarget = current is DevToolsConnection.Connected &&
            current.host == target.displayHost && current.port == target.port &&
            connection != null
        if (isSameTarget) {
            requestInitialState()
            return
        }
        // No explicit disconnect() first: connect() already cancels and awaits any existing
        // connection under lifecycleMutex. Calling both raced, because each is an independent
        // scope.launch with no ordering guarantee on which acquires the mutex first — the
        // disconnect could land *after* the connect and immediately tear it down again.
        connect(target)
    }

    override fun disconnect() {
        scope.launch {
            lifecycleMutex.withLock { cancelConnectionLocked() }
            _state.value = DevToolsConnection.Disconnected
            _switching.value = false
            _currentDeviceId.value = null
            clearAll()
        }
    }

    override fun replayTrace(request: ReplayRequest) {
        // Marked in flight before the send so the button reflects the click immediately. The device
        // always answers with a ReplayResultMessage, success or failure, which clears it.
        replayStore.markInFlight(request.sourceTraceId)
        scope.launch { sendMessage(RequestReplayTraceMessage(request = request)) }
    }

    override fun dismissReplayError(sourceTraceId: String) = replayStore.dismissError(sourceTraceId)

    override fun requestDatabaseSchema(databaseName: String) {
        scope.launch { sendMessage(RequestDatabaseSchemaMessage(databaseName = databaseName)) }
    }

    override fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int) {
        scope.launch {
            sendMessage(RequestDatabaseTableMessage(databaseName = databaseName, tableName = tableName, limit = limit))
        }
    }

    override fun requestPrefValue(key: String) {
        scope.launch { sendMessage(RequestPrefValueMessage(key = key)) }
    }

    override fun requestInitialState() {
        scope.launch { sendMessage(RequestInitialStateMessage()) }
    }

    override fun clearCaptured(traces: Boolean, events: Boolean) {
        // Clear locally straight away so the UI responds immediately; the device's fresh snapshot
        // arrives moments later and is authoritative.
        if (traces) apiLogStore.clear()
        if (events) eventStore.clear()
        scope.launch { sendMessage(RequestClearMessage(traces = traces, events = events)) }
    }

    override fun markTraceViewed(id: String) = apiLogStore.markViewed(id)

    override fun markEventViewed(id: Long) = eventStore.markViewed(id)

    override fun submitOtp(otp: String) {
        scope.launch { sendMessage(AuthResponseMessage(otp = otp)) }
    }

    private suspend fun sendMessage(message: DevToolsMessage) {
        val frame = DevToolsProtocol.encodeEnvelope(message)
        writeMutex.withLock {
            connection?.write(frame)
        }
    }

    private fun handleMessage(message: DevToolsMessage) {
        when (message) {
            is AuthSuccessMessage -> {
                // Non-null only after a fresh OTP pairing; a token-authenticated reconnect
                // returns null because there is nothing new to store.
                message.token?.let { DesktopTrustPrefs.save(_currentDeviceId.value, it) }
                val current = _state.value
                if (current is DevToolsConnection.AwaitingAuth) {
                    _state.value = DevToolsConnection.Connected(current.host, current.port)
                    scope.launch { sendMessage(RequestInitialStateMessage()) }
                }
            }

            is AuthOtpRequiredMessage -> {
                val current = _state.value
                if (current is DevToolsConnection.AwaitingAuth) {
                    _state.value = current.copy(otpRequired = true)
                }
            }

            is AuthFailureMessage -> {
                // Drop the stored token. If the device revoked its tokens (app data cleared,
                // reinstalled) a stale one fails forever, and without this the user has no way
                // back to the OTP prompt.
                DesktopTrustPrefs.forget(_currentDeviceId.value)
                _state.value = DevToolsConnection.Failed(message.reason)
            }

            is InitialStateMessage -> {
                val payload = message.payload
                eventStore.replace(payload.events)
                apiLogStore.replace(payload.apiLogs)
                databaseStore.replaceDatabases(
                    payload.databases.map { it.toDomain() },
                    payload.selectedDatabase,
                )
                databaseStore.replaceSchema(payload.databaseSchema.toDomain())
                prefsStore.replaceKeys(payload.preferenceKeys)
                buildInfoStore.replace(payload.buildInfo?.toDomain())
                chronicleStore.replace(payload.chronicle.map { it.toDomain() })
                replayStore.setSupported(payload.replaySupported)
            }

            is ReplayResultMessage -> {
                if (message.sent) {
                    // Nothing to render here: the replay's own trace arrives over STREAM_API_LOG
                    // like any other request, carrying the status and response body.
                    replayStore.markSucceeded(message.sourceTraceId)
                } else {
                    replayStore.markFailed(
                        message.sourceTraceId,
                        message.error ?: "Replay failed on the device.",
                    )
                }
            }

            is StreamEventMessage -> eventStore.append(message.event)

            is StreamApiLogMessage -> apiLogStore.append(message.log)

            is DatabaseSnapshotMessage -> {
                databaseStore.applySnapshot(
                    message.payload.schema?.toDomain(),
                    message.payload.table?.toDomain(),
                )
            }

            is PrefsSnapshotMessage -> {
                prefsStore.applySnapshot(message.payload.keys, message.payload.values)
            }

            else -> Unit
        }
    }

    private fun clearAll() {
        eventStore.clear()
        apiLogStore.clear()
        databaseStore.clear()
        prefsStore.clear()
        buildInfoStore.clear()
        chronicleStore.clear()
        replayStore.clear()
    }

    /**
     * Releases the socket and cancels this repository's scope. Called from window teardown.
     *
     * Deliberately does not call disconnect(): that now dispatches into [scope], which this
     * method is about to cancel, so the teardown would never run.
     */
    fun close() {
        generation++
        connection?.close()
        connection = null
        scope.cancel()
    }


}

private const val INITIAL_RECONNECT_DELAY_MS = 500L
private const val MAX_RECONNECT_DELAY_MS = 5_000L

/**
 * Delay before reconnect attempt [attempt]: 500ms, 1s, 2s, 4s, then every 5s.
 *
 * A free function rather than a private method so the pacing can be tested against the real
 * implementation — a test that restated the formula would pass just as happily if the caller
 * stopped using it.
 */
internal fun reconnectDelayMillis(attempt: Int): Long =
    (INITIAL_RECONNECT_DELAY_MS shl (attempt - 1).coerceIn(0, 4))
        .coerceAtMost(MAX_RECONNECT_DELAY_MS)
