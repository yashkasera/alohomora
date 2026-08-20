package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.CacheRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheDeleteUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheRefreshUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestCacheUpdateUseCase
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
    private val requestCacheUpdateUseCase: RequestCacheUpdateUseCase,
    private val requestCacheDeleteUseCase: RequestCacheDeleteUseCase,
    private val requestCacheRefreshUseCase: RequestCacheRefreshUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val requested = mutableSetOf<String>()

    val uiState: StateFlow<CacheUiState> =
        combine(repository.state, _query) { state, query ->
            CacheUiState(
                rows = state.toCacheRows(query),
                query = query,
                totalCount = state.keys.size,
                loadedCount = state.keys.count { state.values.containsKey(it) },
                hasStoreData = state.stores.isNotEmpty(),
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), CacheUiState())

    init {
        scope.launch {
            repository.state.collect { state ->
                if (state.stores.isNotEmpty()) return@collect
                requested.retainAll(state.keys.toSet())
                val missing =
                    state.keys.filter { it !in requested && !state.values.containsKey(it) }
                if (missing.isEmpty()) return@collect
                requested += missing
                missing.forEach(requestCacheValueUseCase::invoke)
            }
        }
    }

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun updateValue(storeName: String, key: String, newValue: String?, type: String) {
        requestCacheUpdateUseCase(storeName, key, newValue, type)
    }

    fun deleteValue(storeName: String, key: String) {
        requestCacheDeleteUseCase(storeName, key)
    }

    fun refresh() {
        requestCacheRefreshUseCase()
    }

    fun close() {
        scope.cancel()
    }

    private companion object {
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L
    }
}
