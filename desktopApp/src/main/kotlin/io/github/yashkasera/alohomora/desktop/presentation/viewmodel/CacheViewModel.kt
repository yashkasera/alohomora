package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.CacheRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheValueUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.CacheUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.toCacheRows
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CacheViewModel(
    private val repository: CacheRepository,
    private val requestCacheValueUseCase: RequestCacheValueUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /**
     * Keys already asked for, so no key is requested twice.
     *
     * A plain set rather than a `StateFlow`: it is read and written only by the single collector in
     * [init], so it never crosses threads, and nothing renders it — a pending row is derived from the
     * store instead (`CacheRow.isLoaded`), which stays true even if this bookkeeping is wrong.
     */
    private val requested = mutableSetOf<String>()

    val uiState: StateFlow<CacheUiState> =
        combine(repository.state, _query) { state, query ->
            CacheUiState(
                rows = state.toCacheRows(query),
                query = query,
                totalCount = state.keys.size,
                loadedCount = state.keys.count { state.values.containsKey(it) },
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), CacheUiState())

    init {
        // Values are fetched up front rather than on click, which is what lets the panel be one list
        // instead of a key list and a separate value list. The wire only ever answers one key per
        // `REQUEST_PREF_VALUE`, so "load everything" can only mean one request per key — but each is a
        // tiny frame over a loopback tunnel, and the alternative is a table whose value column stays
        // blank until the user has clicked every row. Search over values depends on this too: an
        // unloaded value cannot be matched.
        scope.launch {
            repository.state.collect { state ->
                // Pruned first, so a store clear on disconnect or device switch lets the same keys be
                // requested again. Without this the set would still hold them and the second session
                // would render every row pending forever.
                requested.retainAll(state.keys.toSet())

                val missing = state.keys.filter { it !in requested && !state.values.containsKey(it) }
                if (missing.isEmpty()) return@collect
                requested += missing
                missing.forEach(requestCacheValueUseCase::invoke)
            }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
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

    private companion object {
        /** Keeps the derivation warm across a quick panel switch; see `TracesViewModel`. */
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
