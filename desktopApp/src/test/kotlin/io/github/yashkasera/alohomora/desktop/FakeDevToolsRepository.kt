package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.PluginDataSnapshot
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.GitHistoryCommit
import io.github.yashkasera.alohomora.desktop.domain.model.ReplayState
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * An in-memory [DevToolsRepository] for driving whole panels in a composition.
 *
 * Worth its thirty-odd members exactly once: the Events sheet shipped a layout crash that only appeared
 * when the real panel and the real sheet were composed together, and no pure test could reach it. Every
 * command is recorded rather than ignored so a test can also assert what the UI asked the device to do.
 */
class FakeDevToolsRepository(
    events: List<Event> = emptyList(),
    cache: CacheState = CacheState(),
) : DevToolsRepository {

    private val _events = MutableStateFlow(events)
    private val _cacheState = MutableStateFlow(cache)

    override val events: StateFlow<List<Event>> = _events
    override val cacheState: StateFlow<CacheState> = _cacheState

    override val connectionState =
        MutableStateFlow<DevToolsConnection>(DevToolsConnection.Disconnected)
    override val currentDeviceId = MutableStateFlow<String?>("test-device")
    override val switching = MutableStateFlow(false)
    override val errors = MutableStateFlow<List<Error>>(emptyList())
    override val spans = MutableStateFlow<List<Span>>(emptyList())
    override val spanCaptureSupported = MutableStateFlow(false)
    override val networkRulesSupported = MutableStateFlow(false)
    override val vpnThrottleSupported = MutableStateFlow(false)
    override val vpnState = MutableStateFlow(VpnThrottleState.OFF)
    override val traffic = MutableStateFlow<List<TrafficEntry>>(emptyList())
    override val databaseSnapshot = MutableStateFlow(DatabaseSnapshot())
    override val featureFlags = MutableStateFlow<List<FeatureFlag>>(emptyList())
    override val pluginData = MutableStateFlow<List<PluginDataSnapshot>>(emptyList())
    override val buildInfo = MutableStateFlow<BuildInfo?>(null)
    override val gitHistory = MutableStateFlow<List<GitHistoryCommit>>(emptyList())
    override val replayState = MutableStateFlow(ReplayState())
    override val deviceError = MutableStateFlow<String?>(null)

    /** Keys whose value the UI asked for, in order. */
    val requestedCacheKeys = mutableListOf<String>()
    val viewedEventIds = mutableListOf<Long>()
    var clearedEvents = false
        private set

    /** Mirrors the real store: marking replaces the instance rather than mutating it. */
    override fun markEventViewed(id: Long) {
        viewedEventIds += id
        _events.value = _events.value.map { if (it.id == id) it.copy(isViewed = true) else it }
    }

    override fun requestCacheValue(key: String) {
        requestedCacheKeys += key
    }

    /** Answers a pending request, the way a `CacheSnapshotMessage` would. */
    fun deliverCacheValue(key: String, value: String?) {
        _cacheState.value = _cacheState.value.copy(
            values = _cacheState.value.values + (key to value),
        )
    }

    /** Records which streams a clear_captured asked to wipe, so a test can assert the mapping. */
    data class ClearCall(
        val traces: Boolean,
        val events: Boolean,
        val errors: Boolean,
        val spans: Boolean,
    )

    val clearCalls = mutableListOf<ClearCall>()

    override fun clearCaptured(traces: Boolean, events: Boolean, errors: Boolean, spans: Boolean) {
        clearCalls += ClearCall(traces, events, errors, spans)
        if (events) {
            clearedEvents = true
            _events.value = emptyList()
        }
    }

    /** Replay requests the UI/agent sent; the fake also marks the source in-flight, like the real store. */
    val replayedRequests = mutableListOf<ReplayRequest>()
    val mockRulesSent = mutableListOf<List<MockRule>>()
    val throttleSet = mutableListOf<ThrottleProfile>()

    override fun replayTraffic(request: ReplayRequest) {
        replayedRequests += request
        replayState.value =
            replayState.value.copy(inFlight = replayState.value.inFlight + request.sourceTraceId)
    }

    /** Answers a pending replay the way a `ReplayResultMessage(sent = true)` would. */
    fun deliverReplaySuccess(sourceTraceId: String, entry: TrafficEntry) {
        traffic.value += entry
        replayState.value =
            replayState.value.copy(inFlight = replayState.value.inFlight - sourceTraceId)
    }

    /** Answers a pending replay the way a `ReplayResultMessage(sent = false)` would. */
    fun deliverReplayFailure(sourceTraceId: String, error: String) {
        replayState.value = replayState.value.copy(
            inFlight = replayState.value.inFlight - sourceTraceId,
            errors = replayState.value.errors + (sourceTraceId to error),
        )
    }

    override fun setThrottleProfile(profile: ThrottleProfile) {
        throttleSet += profile
    }

    override fun setMockRules(rules: List<MockRule>) {
        mockRulesSent += rules
    }

    override fun connect(target: DevToolsTarget) = Unit
    override fun switchDevice(target: DevToolsTarget, deviceId: String?) = Unit
    override fun reconnect() = Unit
    override fun disconnect() = Unit
    override fun submitOtp(otp: String) = Unit
    override fun markTrafficViewed(id: String) = Unit
    override fun markErrorViewed(id: Long) = Unit
    override fun markTraceViewed(traceId: String) = Unit
    override fun requestTraceSpans(traceId: String) = Unit
    override fun dismissReplayError(sourceTraceId: String) = Unit
    override fun dismissDeviceError() = Unit
    override fun requestDatabaseSchema(databaseName: String) = Unit
    override fun setVpnThrottle(profile: ThrottleProfile, enabled: Boolean) = Unit
    override fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int) = Unit
    override fun requestDatabaseUpdate(
        databaseName: String,
        tableName: String,
        primaryKeys: Map<String, String>,
        columnName: String,
        newValue: String?,
    ) = Unit

    override fun requestPluginDataUpdate(pluginId: String, key: String, value: String) = Unit
    override fun requestCacheUpdate(storeName: String, key: String, newValue: String?, type: String) = Unit
    override fun requestCacheDelete(storeName: String, key: String) = Unit
    override fun requestCacheRefresh() = Unit
    override fun requestInitialState() = Unit
}
