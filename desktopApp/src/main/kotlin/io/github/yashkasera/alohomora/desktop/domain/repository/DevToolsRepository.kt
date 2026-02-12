package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.ApiLog
import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.Event
import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import kotlinx.coroutines.flow.StateFlow

interface DevToolsRepository {
    val connectionState: StateFlow<DevToolsConnection>
    val currentDeviceId: StateFlow<String?>
    val switching: StateFlow<Boolean>

    val events: StateFlow<List<Event>>
    val apiLogs: StateFlow<List<ApiLog>>
    val databaseSnapshot: StateFlow<DatabaseSnapshot>
    val prefsState: StateFlow<PrefsState>

    fun connect(host: String, port: Int)
    fun switchDevice(host: String, port: Int, deviceId: String? = null)
    fun disconnect()

    fun requestDatabaseSchema(databaseName: String)
    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200)
    fun requestPrefValue(key: String)
    fun requestInitialState()
}
