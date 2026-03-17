package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OverviewState(
    val serverEnabled: Boolean = false,
    val serverPort: String = "53999",
    val serverError: String? = null,
    val deviceConnectionStatus: String = "UNKNOWN",
)

sealed class OverviewEvent {
    data class ToggleServer(val enabled: Boolean) : OverviewEvent()
    data class PortChanged(val value: String) : OverviewEvent()
}

internal class OverviewViewModel(
    private val devToolsRuntime: DevToolsRuntime,
) : ViewModel() {

    private val _state = MutableStateFlow(OverviewState())
    val state: StateFlow<OverviewState> = _state

    init {
        viewModelScope.launch {
            devToolsRuntime.serverState.collect { serverState ->
                val status = when {
                    serverState.isRunning && serverState.hasClient -> "CONNECTED"
                    serverState.isRunning -> "DISCONNECTED"
                    else -> "OFF"
                }
                _state.value = _state.value.copy(
                    serverEnabled = serverState.isRunning,
                    serverPort = serverState.port?.toString() ?: _state.value.serverPort,
                    serverError = serverState.lastError,
                    deviceConnectionStatus = status,
                )
            }
        }
    }

    fun onEvent(event: OverviewEvent) {
        when (event) {
            is OverviewEvent.ToggleServer -> handleToggle(event.enabled)
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
                val started = Alohomora.startDevToolsServer(port)
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
                Alohomora.stopDevToolsServer()
            } catch (_: Exception) {
                // no-op
            }
            _state.value = _state.value.copy(serverEnabled = false)
        }
    }
}
