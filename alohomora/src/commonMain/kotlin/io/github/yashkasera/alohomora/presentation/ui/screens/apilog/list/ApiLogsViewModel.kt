package io.github.yashkasera.alohomora.presentation.ui.screens.apilog.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.domain.usecase.api.GetNetworkCallsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ApiLogsState(
    val calls: List<ApiRequest> = emptyList(),
)

internal class ApiLogsViewModel(
    getNetworkCallsUseCase: GetNetworkCallsUseCase,
) : ViewModel() {


    val state: StateFlow<ApiLogsState> = getNetworkCallsUseCase()
        .map { calls -> ApiLogsState(calls = calls) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ApiLogsState(emptyList()),
        )
}
