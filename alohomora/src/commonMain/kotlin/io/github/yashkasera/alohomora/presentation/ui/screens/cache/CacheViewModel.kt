package io.github.yashkasera.alohomora.presentation.ui.screens.cache

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.usecase.cache.GetCacheUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class CacheState(
    val entries: List<CacheEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val filteredEntries: List<CacheEntry>
        get() = if (searchQuery.isBlank()) {
            entries
        } else {
            val query = searchQuery.lowercase()
            entries.filter { entry ->
                entry.key.lowercase().contains(query) ||
                    entry.value.lowercase().contains(query)
            }
        }

    val totalEntries: Int get() = entries.size
    val filteredCount: Int get() = filteredEntries.size
}

internal class CacheViewModel(
    private val getPreferencesUseCase: GetCacheUseCase,
) : ViewModel() {

    private val entries = MutableStateFlow<List<CacheEntry>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    val state: StateFlow<CacheState> = combine(
        entries,
        searchQuery,
        isLoading,
        error,
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        CacheState(
            entries = flows[0] as List<CacheEntry>,
            searchQuery = flows[1] as String,
            isLoading = flows[2] as Boolean,
            error = flows[3] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CacheState(),
    )

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                entries.value = getPreferencesUseCase()
            } catch (e: Exception) {
                error.value = e.message ?: "Failed to load preferences"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                entries.value = getPreferencesUseCase.refresh()
            } catch (e: Exception) {
                error.value = e.message ?: "Failed to refresh preferences"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun clearSearch() {
        searchQuery.value = ""
    }

    fun getTotalSize(): String {
        return getPreferencesUseCase.getTotalSize(entries.value)
    }
}
