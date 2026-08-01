package io.github.yashkasera.alohomora.presentation.ui.screens.error.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.domain.usecase.error.GetErrorDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.error.MarkErrorAsViewedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class ErrorDetailsState(
    val error: Error? = null,
    val isLoading: Boolean = true,
)

internal class ErrorDetailsViewModel(
    private val errorId: Long,
    getErrorDetailsUseCase: GetErrorDetailsUseCase,
    private val markErrorAsViewedUseCase: MarkErrorAsViewedUseCase,
) : ViewModel() {

    init {
        markAsViewed()
    }

    val state: StateFlow<ErrorDetailsState> = getErrorDetailsUseCase(errorId)
        .map { error ->
            ErrorDetailsState(
                error = error,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ErrorDetailsState(),
        )

    private fun markAsViewed() {
        viewModelScope.launch {
            markErrorAsViewedUseCase(errorId)
        }
    }
}
