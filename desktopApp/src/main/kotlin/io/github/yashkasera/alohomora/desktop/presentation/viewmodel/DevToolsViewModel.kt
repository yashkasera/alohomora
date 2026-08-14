package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.domain.service.SlackShareService
import io.github.yashkasera.alohomora.desktop.domain.usecase.ConnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.DisconnectDevToolsUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ReplayTrafficUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheValueUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestInitialStateUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SwitchDevToolsDeviceUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.DevToolsUiState
import io.github.yashkasera.alohomora.replay.ReplayRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
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
    private val requestCacheValueUseCase: RequestCacheValueUseCase,
    private val replayTrafficUseCase: ReplayTrafficUseCase,
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

    /** The device's last command failure. Advisory: the session is still up. */
    val deviceError: StateFlow<String?> = repository.deviceError

    fun dismissDeviceError() = repository.dismissDeviceError()

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

    fun markTrafficViewed(id: String) = repository.markTrafficViewed(id)

    fun markEventViewed(id: Long) = repository.markEventViewed(id)

    fun markErrorViewed(id: Long) = repository.markErrorViewed(id)

    fun clearTraffic() = repository.clearCaptured(traces = true)

    fun clearEvents() = repository.clearCaptured(events = true)

    fun clearErrors() = repository.clearCaptured(errors = true)

    fun requestDatabaseSchema(databaseName: String) = requestDatabaseSchemaUseCase(databaseName)

    fun requestDatabaseTable(databaseName: String, tableName: String, limit: Int = 200) {
        requestDatabaseTableUseCase(databaseName, tableName, limit)
    }

    fun requestCacheValue(key: String) = requestCacheValueUseCase(key)

    /**
     * Sends [request] to the device to be re-issued through the app's own HTTP client.
     *
     * Takes a whole [ReplayRequest] rather than a trace id because the caller has already let the
     * user edit the URL, headers and payload. The response is not returned here — it arrives as a
     * new trace in [traffic], stamped with `replayOf`.
     */
    fun replayTraffic(request: ReplayRequest) = replayTrafficUseCase(request)

    fun dismissReplayError(sourceTraceId: String) = repository.dismissReplayError(sourceTraceId)

    private val _slackShareError = MutableStateFlow<String?>(null)
    val slackShareError: StateFlow<String?> = _slackShareError.asStateFlow()

    fun isSlackConfigured(): Boolean = repository.buildInfo.value?.slackWebhookUrl.isNullOrBlank().not()

    fun shareCurlToSlack(trace: TrafficEntry, email: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            val result = slackShareService.shareCurl(trace, email, repository.buildInfo.value)
            result.onSuccess {
                _slackShareError.value = null
                onSuccess()
            }.onFailure { error ->
                _slackShareError.value = error.message
            }
        }
    }

    /**
     * Shares one event. Lives here rather than on [EventsViewModel] because the service and
     * [slackShareError] already do, and the Events sheet reads that error the same way the traffic
     * sheet does.
     */
    fun shareEventToSlack(event: Event, email: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            val result = slackShareService.shareEvent(event, email, repository.buildInfo.value)
            result.onSuccess {
                _slackShareError.value = null
                onSuccess()
            }.onFailure { error ->
                _slackShareError.value = error.message
            }
        }
    }

    fun shareErrorToSlack(error: Error, email: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            val result = slackShareService.shareError(error, email, repository.buildInfo.value)
            result.onSuccess {
                _slackShareError.value = null
                onSuccess()
            }.onFailure { e ->
                _slackShareError.value = e.message
            }
        }
    }

    fun shareTextToSlack(trace: TrafficEntry, email: String, onSuccess: () -> Unit = {}) {
        scope.launch {
            val result = slackShareService.shareText(trace, email, repository.buildInfo.value)
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
    val errors = repository.errors

    private val _errorQuery = MutableStateFlow("")
    val errorQuery: StateFlow<String> = _errorQuery.asStateFlow()

    val filteredErrors: StateFlow<List<Error>> = combine(errors, _errorQuery) { list, q ->
        if (q.isBlank()) list
        else {
            val lower = q.lowercase()
            list.filter { error ->
                error.exceptionTypeName().lowercase().contains(lower) ||
                    error.reason?.lowercase()?.contains(lower) == true ||
                    error.place?.lowercase()?.contains(lower) == true
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onErrorQueryChange(query: String) {
        _errorQuery.value = query
    }
    val traffic = repository.traffic
    val databaseSnapshot = repository.databaseSnapshot
    val cacheState = repository.cacheState
    val buildInfo = repository.buildInfo
    val gitHistory = repository.gitHistory
    val replayState = repository.replayState
}
