package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class OverviewState(
    val isConnected: Boolean = false,
)

sealed class OverviewEvent {
    data class ConnectClicked(val url: String) : OverviewEvent()
}

internal class OverviewViewModel(
) : ViewModel() {

    val state: StateFlow<OverviewState> =
        MutableStateFlow(OverviewState())

    fun onEvent(event: OverviewEvent) {
        when (event) {
            is OverviewEvent.ConnectClicked -> {
            }
        }
    }
}
