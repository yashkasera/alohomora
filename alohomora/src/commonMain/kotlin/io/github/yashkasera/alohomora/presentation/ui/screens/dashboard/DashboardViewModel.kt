package io.github.yashkasera.alohomora.presentation.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import io.github.yashkasera.alohomora.domain.usecase.api.GetLogsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class DashboardState(
//    val logs: List<LogEntity> = emptyList(),
    val isConnected: Boolean = false,
)

sealed class DashboardEvent {
    data class ConnectClicked(val url: String) : DashboardEvent()
}

internal class DashboardViewModel(
    getLogsUseCase: GetLogsUseCase,
//    private val connectToRemoteUseCase: ConnectToRemoteUseCase,
) : ViewModel() {

    val state: StateFlow<DashboardState> =
        MutableStateFlow(DashboardState())

//        getLogsUseCase()
//        .map { logs -> DashboardState(logs = logs) }
//        .stateIn(
//            scope = viewModelScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = DashboardState()
//        )

    fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.ConnectClicked -> {
//                connectToRemoteUseCase(event.url)
            }
        }
    }
}


