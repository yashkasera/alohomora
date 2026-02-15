package io.github.yashkasera.alohomora.presentation.ui.screens.apilog.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.domain.usecase.api.GetApiLogDetailsUseCase
import kotlin.math.log
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ApiLogDetailsState(
    val call: ApiRequest? = null,
)

internal class ApiLogDetailsViewModel(
    private val logId: String,
    getNetworkCallsUseCase: GetApiLogDetailsUseCase,
) : ViewModel() {


    val state: StateFlow<ApiLogDetailsState> =
        getNetworkCallsUseCase(id = logId)
            .map { calls -> ApiLogDetailsState(call = calls) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ApiLogDetailsState(),
            )
}
