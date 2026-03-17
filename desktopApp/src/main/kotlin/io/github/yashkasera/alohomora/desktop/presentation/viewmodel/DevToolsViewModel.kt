package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.ConnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DisconnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestInitialStateUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestPrefValueUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SwitchDevToolsDeviceUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.DevToolsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class DevToolsViewModel(
    private val repository: DevToolsRepository,
    private val connectUseCase: ConnectDevToolsUseCase,
    private val disconnectUseCase: DisconnectDevToolsUseCase,
    private val switchDeviceUseCase: SwitchDevToolsDeviceUseCase,
    private val requestInitialStateUseCase: RequestInitialStateUseCase,
    private val requestDatabaseSchemaUseCase: RequestDatabaseSchemaUseCase,
    private val requestDatabaseTableUseCase: RequestDatabaseTableUseCase,
    private val requestPrefValueUseCase: RequestPrefValueUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val uiState: StateFlow<DevToolsUiState> = combine(
        repository.connectionState,
        repository.currentDeviceId,
        repository.switching,
    ) { connection, deviceId, switching ->
        DevToolsUiState(connection, deviceId, switching)
    }.stateIn(
        scope,
        kotlinx.coroutines.flow.SharingStarted.Eagerly,
        DevToolsUiState(
            connection = repository.connectionState.value,
            currentDeviceId = repository.currentDeviceId.value,
            switching = repository.switching.value,
        )
    )

    fun connect(host: String, port: Int) = connectUseCase(host, port)

    fun disconnect() = disconnectUseCase()

    fun switchDevice(host: String, port: Int, deviceId: String? = null) {
        switchDeviceUseCase(host, port, deviceId)
    }

    fun requestInitialState() = requestInitialStateUseCase()

    fun requestDatabaseSchema(databaseName: String) = requestDatabaseSchemaUseCase(databaseName)

    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200) {
        requestDatabaseTableUseCase(databaseName, tableName, limit)
    }

    fun requestPrefValue(key: String) = requestPrefValueUseCase(key)

    val events = repository.events
    val apiLogs = repository.apiLogs
    val databaseSnapshot = repository.databaseSnapshot
    val prefsState = repository.prefsState
    val buildInfo = repository.buildInfo
    val chronicle = repository.chronicle
}
