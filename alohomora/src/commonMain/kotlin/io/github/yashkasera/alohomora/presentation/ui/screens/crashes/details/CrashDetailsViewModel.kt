package io.github.yashkasera.alohomora.presentation.ui.screens.crashes.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.data.entity.Crash
import io.github.yashkasera.alohomora.domain.usecase.GetCrashDetailsUseCase
import io.github.yashkasera.alohomora.domain.usecase.MarkCrashAsViewedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CrashDetailsState(
    val crash: Crash? = null,
    val isLoading: Boolean = true,
)

internal class CrashDetailsViewModel(
    private val crashId: Long,
    getCrashDetailsUseCase: GetCrashDetailsUseCase,
    private val markCrashAsViewedUseCase: MarkCrashAsViewedUseCase,
) : ViewModel() {

    init {
        markAsViewed()
    }

    val state: StateFlow<CrashDetailsState> = getCrashDetailsUseCase(crashId)
        .map { crash ->
            CrashDetailsState(
                crash = crash,
                isLoading = false,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CrashDetailsState(),
        )

    private fun markAsViewed() {
        viewModelScope.launch {
            markCrashAsViewedUseCase(crashId)
        }
    }
}
