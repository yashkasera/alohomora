package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.ChronicleCommit
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import kotlinx.coroutines.flow.StateFlow

interface DevToolsRepository {
    val connectionState: StateFlow<DevToolsConnection>
    val currentDeviceId: StateFlow<String?>
    val switching: StateFlow<Boolean>

    val events: StateFlow<List<TelemetryEvent>>
    val apiLogs: StateFlow<List<TraceEntry>>
    val databaseSnapshot: StateFlow<DatabaseSnapshot>
    val prefsState: StateFlow<PrefsState>
    val buildInfo: StateFlow<BuildInfo?>
    val chronicle: StateFlow<List<ChronicleCommit>>

    fun connect(host: String, port: Int)
    fun switchDevice(host: String, port: Int, deviceId: String? = null)
    fun disconnect()
    fun submitOtp(otp: String)

    fun requestDatabaseSchema(databaseName: String)
    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200)
    fun requestPrefValue(key: String)
    fun requestInitialState()
}
