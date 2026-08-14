package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.common.MockRule
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
import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.coroutines.flow.StateFlow

interface DevToolsRepository {
    val connectionState: StateFlow<DevToolsConnection>
    val currentDeviceId: StateFlow<String?>
    val switching: StateFlow<Boolean>

    val events: StateFlow<List<Event>>
    val errors: StateFlow<List<Error>>
    val spans: StateFlow<List<Span>>

    /** False until the device reports it, so the panel degrades against an app with no tracer. */
    val spanCaptureSupported: StateFlow<Boolean>

    /** False until the device reports it, so the toolbar hides against an older app. */
    val networkRulesSupported: StateFlow<Boolean>

    /** False until the device reports it. Android-only; always false for iOS devices. */
    val vpnThrottleSupported: StateFlow<Boolean>
    val vpnState: StateFlow<VpnThrottleState>
    val traffic: StateFlow<List<TrafficEntry>>
    val databaseSnapshot: StateFlow<DatabaseSnapshot>
    val cacheState: StateFlow<CacheState>
    val featureFlags: StateFlow<List<FeatureFlag>>
    val buildInfo: StateFlow<BuildInfo?>
    val gitHistory: StateFlow<List<GitHistoryCommit>>
    val replayState: StateFlow<ReplayState>

    /**
     * The device's last reported command failure, or null once acknowledged.
     *
     * Separate from [connectionState]: the session is fine, one request could not be served. Folding
     * it into [DevToolsConnection.Failed] would claim the connection had dropped and send the UI
     * back to a reconnect state it is not in.
     */
    val deviceError: StateFlow<String?>

    fun connect(target: DevToolsTarget)
    fun switchDevice(target: DevToolsTarget, deviceId: String? = null)

    /** Convenience for TCP targets (adb forward, or the iOS Simulator's shared loopback). */
    fun connect(host: String, port: Int) = connect(DevToolsTarget.Tcp(host, port))

    fun switchDevice(host: String, port: Int, deviceId: String? = null) =
        switchDevice(DevToolsTarget.Tcp(host, port), deviceId)

    fun disconnect()
    fun submitOtp(otp: String)

    /**
     * Deletes captured traffic and/or events on the device as well as locally.
     *
     * Device-side, not just local: a local-only clear repopulates from the device snapshot on the
     * next reconnect, which reads as the button not having worked.
     */
    fun clearCaptured(
        traces: Boolean = false,
        events: Boolean = false,
        errors: Boolean = false,
        spans: Boolean = false,
    )

    /** Dims a traffic entry the user opened in this window. */
    fun markTrafficViewed(id: String)

    /** Dims an event the user opened in this window. */
    fun markEventViewed(id: Long)

    fun markErrorViewed(id: Long)

    /** Dims a whole trace once opened; viewing is a trace-level act. */
    fun markTraceViewed(traceId: String)

    /**
     * Asks the device for every span of [traceId].
     *
     * Backfills a trace whose earliest spans fell outside the device's snapshot window. Only send
     * this when the device reported `spanCaptureSupported` — an older app decodes it as an unknown
     * type and never replies.
     */
    fun requestTraceSpans(traceId: String)

    /**
     * Asks the device to re-send [request] through the host app's own HTTP client.
     *
     * The whole request travels, not just a traffic id: the user may have edited the URL, headers or
     * payload, and having the app re-sign an edited payload is the point of the feature.
     */
    fun replayTraffic(request: ReplayRequest)

    /** Clears a replay failure the user has acknowledged. */
    fun dismissReplayError(sourceTraceId: String)

    /** Clears the [deviceError] banner the user has acknowledged. */
    fun dismissDeviceError()

    fun setThrottleProfile(profile: ThrottleProfile)
    fun setMockRules(rules: List<MockRule>)
    fun setVpnThrottle(profile: ThrottleProfile, enabled: Boolean)

    fun requestDatabaseSchema(databaseName: String)
    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200)
    fun requestDatabaseUpdate(
        databaseName: String,
        tableName: String,
        primaryKeys: Map<String, String>,
        columnName: String,
        newValue: String?,
    )

    fun requestCacheValue(key: String)
    fun requestInitialState()
}
