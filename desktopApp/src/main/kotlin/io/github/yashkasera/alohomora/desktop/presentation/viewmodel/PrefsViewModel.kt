package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.PrefsRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestPrefValueUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.PrefsUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class PrefsViewModel(
    private val repository: PrefsRepository,
    private val requestPrefValueUseCase: RequestPrefValueUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val uiState: StateFlow<PrefsUiState> = repository.state
        .map { state -> PrefsUiState(state) }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, PrefsUiState(repository.state.value))

    fun requestPrefValue(key: String) {
        requestPrefValueUseCase(key)
    }

    /**
     * Cancels this view model's scope.
     *
     * Required for per-window teardown: DesktopAppComposition.close() used to cancel
     * only DevToolsViewModel, so every other scope (and its collectors) leaked for the
     * life of the process each time a device window was closed.
     */
    fun close() {
        scope.cancel()
    }
}
