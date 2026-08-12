package io.github.yashkasera.alohomora.presentation.ui.screens.featureflags

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.devtools.FeatureFlagStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
internal data class FeatureFlagsState(
    val flags: List<FeatureFlag> = emptyList(),
    val searchQuery: String = "",
    val selectedSource: String? = null,
) {
    val filteredFlags: List<FeatureFlag>
        get() {
            var result = flags
            if (selectedSource != null) {
                result = result.filter { it.source == selectedSource }
            }
            if (searchQuery.isNotBlank()) {
                val query = searchQuery.lowercase()
                result = result.filter { flag ->
                    flag.key.lowercase().contains(query) ||
                        flag.value.lowercase().contains(query) ||
                        flag.source?.lowercase()?.contains(query) == true ||
                        flag.type?.lowercase()?.contains(query) == true
                }
            }
            return result
        }

    val sources: List<String>
        get() = flags.mapNotNull { it.source }.distinct().sorted()

    val totalCount: Int get() = flags.size
    val filteredCount: Int get() = filteredFlags.size
}

internal class FeatureFlagsViewModel(
    store: FeatureFlagStore,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedSource = MutableStateFlow<String?>(null)

    val state: StateFlow<FeatureFlagsState> = combine(
        store.flags,
        searchQuery,
        selectedSource,
    ) { flagsMap, query, source ->
        FeatureFlagsState(
            flags = flagsMap.values.toList().sortedBy { it.key },
            searchQuery = query,
            selectedSource = source,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FeatureFlagsState(),
    )

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onSourceSelected(source: String?) {
        selectedSource.value = if (selectedSource.value == source) null else source
    }
}
