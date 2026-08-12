package io.github.yashkasera.alohomora.desktop.data.devtools

import io.github.yashkasera.alohomora.common.AuthFailureMessage
import io.github.yashkasera.alohomora.common.AuthOtpRequiredMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.CacheSnapshotMessage
import io.github.yashkasera.alohomora.common.DatabaseSnapshotMessage
import io.github.yashkasera.alohomora.common.DeviceErrorMessage
import io.github.yashkasera.alohomora.common.DevToolsHeartbeat
import io.github.yashkasera.alohomora.common.DevToolsLiveness
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.EnvelopeRead
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.FeatureFlagsSnapshotMessage
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.PingMessage
import io.github.yashkasera.alohomora.common.PongMessage
import io.github.yashkasera.alohomora.common.ReplayResultMessage
import io.github.yashkasera.alohomora.common.RequestCacheValueMessage
import io.github.yashkasera.alohomora.common.RequestClearMessage
import io.github.yashkasera.alohomora.common.RequestDatabaseSchemaMessage
import io.github.yashkasera.alohomora.common.SetMockRulesMessage
import io.github.yashkasera.alohomora.common.SetThrottleProfileMessage
import io.github.yashkasera.alohomora.common.SetVpnThrottleMessage
import io.github.yashkasera.alohomora.common.VpnStateMessage
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.common.RequestDatabaseTableMessage
import io.github.yashkasera.alohomora.common.RequestInitialStateMessage
import io.github.yashkasera.alohomora.common.RequestReplayTraceMessage
import io.github.yashkasera.alohomora.common.RequestTraceSpansMessage
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.StreamErrorMessage
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.StreamEventMessage
import io.github.yashkasera.alohomora.common.StreamSpanMessage
import io.github.yashkasera.alohomora.common.StreamTrafficMessage
import io.github.yashkasera.alohomora.common.TraceSpansSnapshotMessage
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.data.local.BuildMetadataStore
import io.github.yashkasera.alohomora.desktop.data.local.CacheStore
import io.github.yashkasera.alohomora.desktop.data.local.DatabaseSnapshotStore
import io.github.yashkasera.alohomora.desktop.data.local.ErrorStore
import io.github.yashkasera.alohomora.desktop.data.local.FeatureFlagStore
import io.github.yashkasera.alohomora.desktop.data.local.SpanStore
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.data.local.GitHistoryStore
import io.github.yashkasera.alohomora.desktop.data.local.ReplayStore
import io.github.yashkasera.alohomora.desktop.data.local.TrafficStore
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.GitHistoryCommit
import io.github.yashkasera.alohomora.desktop.domain.model.ReplayState
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DevToolsRepositoryImpl(
    private val remoteDataSource: DevToolsRemoteDataSource,
    private val eventStore: EventStore,
    private val errorStore: ErrorStore,
    private val spanStore: SpanStore,
    private val trafficStore: TrafficStore,
    private val databaseStore: DatabaseSnapshotStore,
    private val cacheStore: CacheStore,
    private val featureFlagStore: FeatureFlagStore,
    private val buildMetadataStore: BuildMetadataStore,
    private val gitHistoryStore: GitHistoryStore,
    private val replayStore: ReplayStore,
) : DevToolsRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow<DevToolsConnection>(DevToolsConnection.Disconnected)
    override val connectionState: StateFlow<DevToolsConnection> = _state.asStateFlow()
    private val _currentDeviceId = MutableStateFlow<String?>(null)
    override val currentDeviceId: StateFlow<String?> = _currentDeviceId.asStateFlow()
    private val _switching = MutableStateFlow(false)
    override val switching: StateFlow<Boolean> = _switching.asStateFlow()
    private val _deviceError = MutableStateFlow<String?>(null)
    override val deviceError: StateFlow<String?> = _deviceError.asStateFlow()

    private val _networkRulesSupported = MutableStateFlow(false)
    override val networkRulesSupported: StateFlow<Boolean> = _networkRulesSupported.asStateFlow()

    private val _vpnThrottleSupported = MutableStateFlow(false)
    override val vpnThrottleSupported: StateFlow<Boolean> = _vpnThrottleSupported.asStateFlow()
    private val _vpnState = MutableStateFlow(VpnThrottleState.OFF)
    override val vpnState: StateFlow<VpnThrottleState> = _vpnState.asStateFlow()

    override val events: StateFlow<List<Event>> = eventStore.events
    override val errors: StateFlow<List<Error>> = errorStore.errors
    override val spans: StateFlow<List<Span>> = spanStore.spans
    override val spanCaptureSupported: StateFlow<Boolean> = spanStore.captureSupported
    override val traffic: StateFlow<List<TrafficEntry>> = trafficStore.logs
    override val databaseSnapshot: StateFlow<DatabaseSnapshot> = databaseStore.snapshot
    override val cacheState: StateFlow<CacheState> = cacheStore.state
    override val featureFlags: StateFlow<List<FeatureFlag>> = featureFlagStore.flags
    override val buildInfo: StateFlow<BuildInfo?> = buildMetadataStore.buildInfo
    override val gitHistory: StateFlow<List<GitHistoryCommit>> = gitHistoryStore.commits
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
                        val outcome = runSession(target, host, port, myGeneration)
                        if (generation != myGeneration || !isActive) return@launch

                        // Some failures cannot be retried into working. Reconnecting on a wire
                        // version mismatch just hides the one thing the developer needs to read.
                        if (outcome is SessionOutcome.Fatal) {
                            _state.value = DevToolsConnection.Failed(outcome.reason)
                            return@launch
                        }
                        // A handler already reported something terminal, an invalid OTP being the
                        // one that matters. Without this the Failed state it set was immediately
                        // overwritten with Reconnecting and the rejection flickered past unread.
                        if (_state.value is DevToolsConnection.Failed) return@launch

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


    /** Whether the caller should reconnect, or stop and show the user why. */
    private sealed interface SessionOutcome {
        /** An ordinary drop. Worth another attempt. */
        data object Retryable : SessionOutcome

        /** Reconnecting cannot succeed; only a change on one side can. */
        data class Fatal(val reason: String) : SessionOutcome
    }

    /**
     * Runs one connection attempt to completion.
     *
     * Returns when the socket closes for any reason, reporting whether a retry could help. Never
     * throws for an ordinary drop — a failed attempt is normal here, not exceptional.
     *
     * @return whether this attempt reached AUTH_SUCCESS. A bare TCP connect is not proof of a
     *   reachable device: an iOS app suspended in the background still has its socket bound, so
     *   the connect completes out of the kernel's listen backlog and then hangs. Only the
     *   handshake completing means something on the far side is running.
     */
    private suspend fun runSession(
        target: DevToolsTarget,
        host: String,
        port: Int,
        myGeneration: Int,
    ): SessionOutcome {
        var socket: DevToolsSocket? = null
        try {
            val live = when (target) {
                is DevToolsTarget.Tcp -> remoteDataSource.connect(target.host, target.port)
                // usbmuxd hands back a socket already tunnelled to the device port, so there is
                // no host-side forward to set up first.
                is DevToolsTarget.Usbmux ->
                    remoteDataSource.connectOverUsbmux(target.usbmuxDeviceId, target.port)
            }
            socket = live
            // A newer connect may have superseded us while we were connecting.
            if (generation != myGeneration) return SessionOutcome.Retryable
            connection = socket
            _state.value = DevToolsConnection.AwaitingAuth(host, port)
            _switching.value = false
            // Offer whatever token we hold for this device before the user is asked for
            // anything. Sent unconditionally — an empty probe is how the device learns it must
            // display a code, so skipping it when we have no token would leave the prompt hidden
            // until the device's grace timer fired.
            sendMessage(
                AuthResponseMessage(
                    token = DesktopTrustPrefs.tokenFor(_currentDeviceId.value),
                    // Opts this session into PING/PONG. The device pings only a client that says
                    // it will answer, so omitting this leaves the device unable to reclaim its
                    // single connection slot when this process dies without a FIN.
                    heartbeatSupported = true,
                ),
            )
            return when (val read = remoteDataSource.processConnection(socket, ::handleMessage)) {
                is EnvelopeRead.VersionMismatch -> SessionOutcome.Fatal(
                    "This app's Alohomora SDK speaks DevTools protocol v${read.peerVersion}, " +
                        "this desktop speaks v${read.localVersion}. " +
                        "Update whichever of the two is older.",
                )

                // Retryable on purpose. A desynced or truncated frame is usually transient, and a
                // reconnect resynchronises; only a version difference is known to be permanent.
                is EnvelopeRead.Malformed -> {
                    println("[Alohomora] Connection dropped, malformed frame: ${read.reason}")
                    SessionOutcome.Retryable
                }

                else -> SessionOutcome.Retryable
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Swallowed on purpose: the caller retries, and surfacing every failed attempt as
            // Failed would flicker the UI between error and reconnecting once a second.
            println("[Alohomora] Connection dropped: ${e::class.simpleName}: ${e.message}")
            return SessionOutcome.Retryable
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

    /**
     * Drops the session once the device has gone silent, so the retry loop can rebuild it.
     *
     * The mirror image of the device's own reaper, and the same argument applies in reverse: a
     * device that stops servicing its socket without closing it — an iOS app the OS suspended, a
     * transport that went away — leaves this side parked in a read that never returns, showing
     * "Connected" with zero throughput and no way back but a manual reconnect.
     *
     * Armed by the first PING rather than on connect. A device that predates the heartbeat sends
     * nothing at all while idle, and enforcing silence against it would tear down healthy sessions
     * every [DevToolsHeartbeat.SILENCE_TIMEOUT_MILLIS] for as long as nobody used the app.
     */
    private suspend fun watchSessionForSilence(
        liveness: DevToolsLiveness,
        firstPing: Deferred<Unit>,
        socket: DevToolsSocket,
    ) {
        firstPing.await()
        while (true) {
            delay(DevToolsHeartbeat.PING_INTERVAL_MILLIS)
            if (liveness.isPeerSilent()) {
                println(
                    "[Alohomora] Device silent for ${liveness.silentForMillis()}ms; " +
                        "dropping the session to reconnect.",
                )
                // Closing is what ends the session: it fails the parked read, which returns from
                // processConnection and hands control back to the retry loop.
                socket.close()
                return
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

    override fun setThrottleProfile(profile: ThrottleProfile) {
        scope.launch { sendMessage(SetThrottleProfileMessage(profile = profile)) }
    }

    override fun setMockRules(rules: List<MockRule>) {
        scope.launch { sendMessage(SetMockRulesMessage(rules = rules)) }
    }

    override fun setVpnThrottle(profile: ThrottleProfile, enabled: Boolean) {
        scope.launch { sendMessage(SetVpnThrottleMessage(profile = profile, enabled = enabled)) }
    }

    override fun replayTraffic(request: ReplayRequest) {
        // Marked in flight before the send so the button reflects the click immediately. The device
        // always answers with a ReplayResultMessage, success or failure, which clears it.
        replayStore.markInFlight(request.sourceTraceId)
        scope.launch { sendMessage(RequestReplayTraceMessage(request = request)) }
    }

    override fun dismissReplayError(sourceTraceId: String) = replayStore.dismissError(sourceTraceId)

    override fun dismissDeviceError() {
        _deviceError.value = null
    }

    override fun requestDatabaseSchema(databaseName: String) {
        scope.launch { sendMessage(RequestDatabaseSchemaMessage(databaseName = databaseName)) }
    }

    override fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int) {
        scope.launch {
            sendMessage(RequestDatabaseTableMessage(databaseName = databaseName, tableName = tableName, limit = limit))
        }
    }

    override fun requestCacheValue(key: String) {
        scope.launch { sendMessage(RequestCacheValueMessage(key = key)) }
    }

    override fun requestInitialState() {
        scope.launch { sendMessage(RequestInitialStateMessage()) }
    }

    override fun clearCaptured(traces: Boolean, events: Boolean, errors: Boolean, spans: Boolean) {
        // Clear locally straight away so the UI responds immediately; the device's fresh snapshot
        // arrives moments later and is authoritative.
        if (traces) trafficStore.clear()
        if (events) eventStore.clear()
        if (errors) errorStore.clear()
        if (spans) spanStore.clear()
        scope.launch {
            sendMessage(
                RequestClearMessage(
                    traces = traces,
                    events = events,
                    errors = errors,
                    spans = spans,
                ),
            )
        }
    }

    override fun markTrafficViewed(id: String) = trafficStore.markViewed(id)

    override fun markEventViewed(id: Long) = eventStore.markViewed(id)

    override fun markErrorViewed(id: Long) = errorStore.markViewed(id)

    override fun markTraceViewed(traceId: String) = spanStore.markTraceViewed(traceId)

    override fun requestTraceSpans(traceId: String) {
        // Gated: an app predating span capture decodes this as UnknownMessage and never answers,
        // so an ungated request would leave the panel waiting on a reply that is not coming.
        if (!spanStore.captureSupported.value) return
        scope.launch { sendMessage(RequestTraceSpansMessage(traceId = traceId)) }
    }

    override fun submitOtp(otp: String) {
        scope.launch { sendMessage(AuthResponseMessage(otp = otp, heartbeatSupported = true)) }
    }

    private suspend fun sendMessage(message: DevToolsMessage) {
        val frame = DevToolsProtocol.encodeEnvelope(message)
        writeMutex.withLock {
            connection?.write(frame)
        }
    }

    private suspend fun handleMessage(message: DevToolsMessage) {
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
                withContext(Dispatchers.Default) {
                    val payload = message.payload
                    eventStore.replace(payload.events)
                errorStore.replace(payload.errors)
                    spanStore.replace(payload.spans)
                    spanStore.setCaptureSupported(payload.spanCaptureSupported)
                    _networkRulesSupported.value = payload.networkRulesSupported
                    _vpnThrottleSupported.value = payload.vpnThrottleSupported
                    _vpnState.value = payload.vpnThrottleState
                    trafficStore.replace(payload.traffic)
                    databaseStore.replaceDatabases(
                        payload.databases.map { it.toDomain() },
                        payload.selectedDatabase,
                    )
                    databaseStore.replaceSchema(payload.databaseSchema.toDomain())
                    cacheStore.replaceKeys(payload.cacheKeys)
                    featureFlagStore.replace(payload.featureFlags)
                    buildMetadataStore.replace(payload.buildMetadata?.toDomain())
                    gitHistoryStore.replace(payload.gitHistory.map { it.toDomain() })
                    replayStore.setSupported(payload.replaySupported)
                }
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

            is StreamEventMessage -> {
                // Dispatch to Default dispatcher to prevent blocking on large batches.
                withContext(Dispatchers.Default) {
                    eventStore.append(message.event)
                }
            }

            is StreamErrorMessage -> {
                withContext(Dispatchers.Default) {
                    errorStore.append(message.error)
                }
            }

            is StreamSpanMessage -> {
                withContext(Dispatchers.Default) {
                    spanStore.append(message.span)
                }
            }

            is TraceSpansSnapshotMessage -> {
                withContext(Dispatchers.Default) {
                    spanStore.mergeTrace(message.traceId, message.spans)
                }
            }

            is StreamTrafficMessage -> {
                // Dispatch to Default dispatcher to prevent blocking on large batches.
                withContext(Dispatchers.Default) {
                    trafficStore.append(message.traffic)
                }
            }

            is DatabaseSnapshotMessage -> {
                withContext(Dispatchers.Default) {
                    databaseStore.applySnapshot(
                        message.payload.schema?.toDomain(),
                        message.payload.table?.toDomain(),
                    )
                }
            }

            is CacheSnapshotMessage -> {
                withContext(Dispatchers.Default) {
                    cacheStore.applySnapshot(message.payload.keys, message.payload.values)
                }
            }

            is FeatureFlagsSnapshotMessage -> {
                withContext(Dispatchers.Default) {
                    featureFlagStore.replace(message.flags)
                }
            }

            is VpnStateMessage -> {
                _vpnState.value = message.state
            }

            is DeviceErrorMessage -> {
                // Not a connection failure: the socket is healthy, one command could not be
                // served. Surfaced so a device that cannot read its own database says so instead
                // of looking like an idle console.
                println("[Alohomora] Device failed ${message.request}: ${message.message}")
                _deviceError.value = "${message.request} failed on the device: ${message.message}"
            }

            else -> Unit
        }
    }

    private fun clearAll() {
        _deviceError.value = null
        _networkRulesSupported.value = false
        _vpnThrottleSupported.value = false
        _vpnState.value = VpnThrottleState.OFF
        eventStore.clear()
        trafficStore.clear()
        databaseStore.clear()
        cacheStore.clear()
        featureFlagStore.clear()
        buildMetadataStore.clear()
        gitHistoryStore.clear()
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

/**
 * The attempt number to pace the next reconnect by.
 *
 * A session that got as far as AUTH_SUCCESS proves the device is reachable, so the backoff starts
 * over rather than continuing to climb. The counter used to live outside the retry loop and never
 * reset, so a session that ran for an hour and then dropped resumed at the 5s cap — the slowest
 * possible retry for the case with the best odds of succeeding immediately.
 *
 * Reset to 1, not 0: [reconnectDelayMillis] is 1-based, and the loop reports this number to the UI
 * as "reconnecting (n)".
 */
internal fun nextReconnectAttempt(previous: Int, sessionEstablished: Boolean): Int =
    if (sessionEstablished) 1 else previous + 1
