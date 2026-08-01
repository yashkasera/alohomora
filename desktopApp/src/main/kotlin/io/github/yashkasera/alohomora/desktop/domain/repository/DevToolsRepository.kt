package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.TrafficEntry
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
    val traffic: StateFlow<List<TrafficEntry>>
    val databaseSnapshot: StateFlow<DatabaseSnapshot>
    val cacheState: StateFlow<CacheState>
    val buildInfo: StateFlow<BuildInfo?>
    val gitHistory: StateFlow<List<GitHistoryCommit>>
    val replayState: StateFlow<ReplayState>

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
    fun clearCaptured(traces: Boolean = false, events: Boolean = false)

    /** Dims a traffic entry the user opened in this window. */
    fun markTrafficViewed(id: String)

    /** Dims an event the user opened in this window. */
    fun markEventViewed(id: Long)

    /**
     * Asks the device to re-send [request] through the host app's own HTTP client.
     *
     * The whole request travels, not just a traffic id: the user may have edited the URL, headers or
     * payload, and having the app re-sign an edited payload is the point of the feature.
     */
    fun replayTraffic(request: ReplayRequest)

    /** Clears a replay failure the user has acknowledged. */
    fun dismissReplayError(sourceTraceId: String)

    fun requestDatabaseSchema(databaseName: String)
    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200)
    fun requestCacheValue(key: String)
    fun requestInitialState()
}
