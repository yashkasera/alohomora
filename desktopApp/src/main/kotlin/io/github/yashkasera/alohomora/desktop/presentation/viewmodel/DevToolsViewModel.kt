package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.domain.service.SlackShareService
import io.github.yashkasera.alohomora.desktop.domain.usecase.ConnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DisconnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestInitialStateUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestPrefValueUseCase
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.usecase.ReplayTraceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SwitchDevToolsDeviceUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.DevToolsUiState
import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevToolsViewModel(
    private val repository: DevToolsRepository,
    private val connectUseCase: ConnectDevToolsUseCase,
    private val disconnectUseCase: DisconnectDevToolsUseCase,
    private val switchDeviceUseCase: SwitchDevToolsDeviceUseCase,
    private val requestInitialStateUseCase: RequestInitialStateUseCase,
    private val requestDatabaseSchemaUseCase: RequestDatabaseSchemaUseCase,
    private val requestDatabaseTableUseCase: RequestDatabaseTableUseCase,
    private val requestPrefValueUseCase: RequestPrefValueUseCase,
    private val replayTraceUseCase: ReplayTraceUseCase,
    private val slackShareService: SlackShareService,
) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    val uiState: StateFlow<DevToolsUiState> = combine(
        repository.connectionState,
        repository.currentDeviceId,
        repository.switching,
    ) { connection, deviceId, switching ->
        DevToolsUiState(connection, deviceId, switching)
    }.stateIn(
        scope,
        kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        DevToolsUiState(
            connection = repository.connectionState.value,
            currentDeviceId = repository.currentDeviceId.value,
            switching = repository.switching.value,
        )
    )

    fun connect(host: String, port: Int) = connectUseCase(host, port)

    fun disconnect() = disconnectUseCase()

    fun switchDevice(target: DevToolsTarget, deviceId: String? = null) {
        switchDeviceUseCase(target, deviceId)
    }

    fun switchDevice(host: String, port: Int, deviceId: String? = null) {
        switchDeviceUseCase(host, port, deviceId)
    }

    fun requestInitialState() = requestInitialStateUseCase()

    fun submitOtp(otp: String) = repository.submitOtp(otp)

    fun markTraceViewed(id: String) = repository.markTraceViewed(id)

    fun markEventViewed(id: Long) = repository.markEventViewed(id)

    fun clearApiLogs() = repository.clearCaptured(traces = true)

    fun clearEvents() = repository.clearCaptured(events = true)

    fun requestDatabaseSchema(databaseName: String) = requestDatabaseSchemaUseCase(databaseName)

    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200) {
        requestDatabaseTableUseCase(databaseName, tableName, limit)
    }

    fun requestPrefValue(key: String) = requestPrefValueUseCase(key)

    /**
     * Sends [request] to the device to be re-issued through the app's own HTTP client.
     *
     * Takes a whole [ReplayRequest] rather than a trace id because the caller has already let the
     * user edit the URL, headers and payload. The response is not returned here — it arrives as a
     * new trace in [apiLogs], stamped with `replayOf`.
     */
    fun replayTrace(request: ReplayRequest) = replayTraceUseCase(request)

    fun dismissReplayError(sourceTraceId: String) = repository.dismissReplayError(sourceTraceId)

    private val _slackShareError = MutableStateFlow<String?>(null)
    val slackShareError: StateFlow<String?> = _slackShareError.asStateFlow()

    fun isSlackConfigured(): Boolean = buildInfo.value?.slackWebhookUrl.isNullOrBlank().not()

    fun shareCurlToSlack(trace: TraceEntry, email: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            val result = slackShareService.shareCurl(trace, email, buildInfo.value)
            result.onSuccess {
                _slackShareError.value = null
                onSuccess()
            }.onFailure { error ->
                _slackShareError.value = error.message
            }
        }
    }

    fun shareTextToSlack(trace: TraceEntry, email: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            val result = slackShareService.shareText(trace, email, buildInfo.value)
            result.onSuccess {
                _slackShareError.value = null
                onSuccess()
            }.onFailure { error ->
                _slackShareError.value = error.message
            }
        }
    }

    fun clearSlackShareError() {
        _slackShareError.value = null
    }

    fun close() {
        job.cancel()
    }

    val events = repository.events
    val apiLogs = repository.apiLogs
    val databaseSnapshot = repository.databaseSnapshot
    val prefsState = repository.prefsState
    val buildInfo = repository.buildInfo
    val chronicle = repository.chronicle
    val replayState = repository.replayState
}
