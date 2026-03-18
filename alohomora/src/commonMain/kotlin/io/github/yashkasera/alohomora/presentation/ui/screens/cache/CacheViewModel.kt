package io.github.yashkasera.alohomora.presentation.ui.screens.cache

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.domain.model.PreferenceEntry
import io.github.yashkasera.alohomora.domain.usecase.cache.GetPreferencesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the Cache/Preferences screen.
 *
 * @property entries All preference entries
 * @property searchQuery Current search filter
 * @property isLoading Whether data is being loaded
 * @property error Error message if loading failed
 * @property isLiveMonitoring Whether live monitoring is active
 */
@Immutable
data class CacheState(
    val entries: List<PreferenceEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /**
     * Filtered entries based on search query (matches key or value).
     */
    val filteredEntries: List<PreferenceEntry>
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

/**
 * ViewModel for the Cache/Preferences screen.
 * Manages loading preferences and search/filter functionality.
 */
internal class CacheViewModel(
    private val getPreferencesUseCase: GetPreferencesUseCase,
) : ViewModel() {

    private val entries = MutableStateFlow<List<PreferenceEntry>>(emptyList())
    private val searchQuery = MutableStateFlow("")
    private val isLoading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    /**
     * Combined state flow for the UI.
     */
    val state: StateFlow<CacheState> = combine(
        entries,
        searchQuery,
        isLoading,
        error,
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        CacheState(
            entries = flows[0] as List<PreferenceEntry>,
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

    /**
     * Loads preferences from the repository.
     */
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

    /**
     * Refreshes the preferences list.
     */
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

    /**
     * Updates the search query for filtering.
     */
    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    /**
     * Clears the search query.
     */
    fun clearSearch() {
        searchQuery.value = ""
    }

    /**
     * Calculates the total size of all entries.
     */
    fun getTotalSize(): String {
        return getPreferencesUseCase.getTotalSize(entries.value)
    }
}
