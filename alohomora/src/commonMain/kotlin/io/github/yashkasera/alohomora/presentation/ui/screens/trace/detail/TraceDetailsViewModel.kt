package io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.domain.usecase.trace.GetTraceDetailsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class TraceDetailsState(
    val trace: TraceEntry? = null,
)

internal class TraceDetailsViewModel(
    private val traceId: String,
    getTraceDetailsUseCase: GetTraceDetailsUseCase,
) : ViewModel() {

    val state: StateFlow<TraceDetailsState> =
        getTraceDetailsUseCase(id = traceId)
            .map { trace -> TraceDetailsState(trace = trace) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TraceDetailsState(),
            )
}
