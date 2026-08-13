package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.AttentionItem
import io.github.yashkasera.alohomora.common.mergeAttentionItems
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal enum class DevConnectionStatus { Off, Disconnected, AwaitingAuth, Connected }

@Immutable
internal data class OverviewState(
    val serverEnabled: Boolean = false,
    val serverPort: String = DevToolsDefaults.DEFAULT_PORT.toString(),
    val serverError: String? = null,
    val deviceConnectionStatus: DevConnectionStatus = DevConnectionStatus.Off,
    val pendingOtp: String? = null,
    val rememberDevice: Boolean = false,
    val attentionItems: List<AttentionItem> = emptyList(),
)

internal sealed class OverviewEvent {
    data class ToggleServer(val enabled: Boolean) : OverviewEvent()
    data class PortChanged(val value: String) : OverviewEvent()
    data class RememberDeviceChanged(val remember: Boolean) : OverviewEvent()
}

internal class OverviewViewModel(
    private val devToolsRuntime: DevToolsRuntime,
    private val errorRepository: ErrorRepository,
    private val trafficRepository: TrafficRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OverviewState())
    val state: StateFlow<OverviewState> = _state

    init {
        viewModelScope.launch {
            devToolsRuntime.serverState.collect { serverState ->
                val status = when {
                    serverState.isRunning && serverState.hasClient && serverState.pendingOtp != null -> DevConnectionStatus.AwaitingAuth
                    serverState.isRunning && serverState.hasClient -> DevConnectionStatus.Connected
                    serverState.isRunning -> DevConnectionStatus.Disconnected
                    else -> DevConnectionStatus.Off
                }
                _state.value = _state.value.copy(
                    serverEnabled = serverState.isRunning,
                    serverPort = serverState.port?.toString() ?: _state.value.serverPort,
                    serverError = serverState.lastError,
                    deviceConnectionStatus = status,
                    pendingOtp = serverState.pendingOtp,
                    rememberDevice = serverState.rememberDevice,
                )
            }
        }
        viewModelScope.launch {
            combine(
                errorRepository.observeUnviewed(),
                trafficRepository.observeUnviewedFailed(),
            ) { errors, traffic ->
                mergeAttentionItems(errors, traffic)
            }.collect { items ->
                _state.value = _state.value.copy(attentionItems = items)
            }
        }
    }

    fun onEvent(event: OverviewEvent) {
        when (event) {
            is OverviewEvent.ToggleServer -> handleToggle(event.enabled)
            is OverviewEvent.RememberDeviceChanged ->
                devToolsRuntime.setRememberDevice(event.remember)
            is OverviewEvent.PortChanged -> {
                _state.value = _state.value.copy(serverPort = event.value, serverError = null)
            }
        }
    }

    private fun handleToggle(enabled: Boolean) {
        if (enabled) {
            val port = _state.value.serverPort.toIntOrNull()
            if (port == null || port <= 0) {
                _state.value = _state.value.copy(serverEnabled = false, serverError = "Invalid port")
                return
            }
            try {
                val started = devToolsRuntime.start(port)
                _state.value = _state.value.copy(
                    serverEnabled = started,
                    serverError = if (started) null else "Failed to start server",
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    serverEnabled = false,
                    serverError = e.message ?: "Failed to start server",
                )
            }
        } else {
            try {
                devToolsRuntime.stop()
            } catch (_: Exception) {
                /* no-op */
            }
            _state.value = _state.value.copy(serverEnabled = false)
        }
    }
}
