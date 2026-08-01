package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.CacheRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheValueUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.CacheUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CacheViewModel(
    private val repository: CacheRepository,
    private val requestCacheValueUseCase: RequestCacheValueUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val uiState: StateFlow<CacheUiState> = repository.state
        .map { state -> CacheUiState(state) }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, CacheUiState(repository.state.value))

    fun requestCacheValue(key: String) {
        requestCacheValueUseCase(key)
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
