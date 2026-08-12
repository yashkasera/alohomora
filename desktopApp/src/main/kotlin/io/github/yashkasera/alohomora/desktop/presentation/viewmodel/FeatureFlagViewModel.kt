package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class FeatureFlagUiState(
    val flags: List<FeatureFlag> = emptyList(),
    val query: String = "",
    val selectedSource: String? = null,
    val totalCount: Int = 0,
    val sources: List<String> = emptyList(),
) {
    val filteredFlags: List<FeatureFlag>
        get() {
            var result = flags
            if (selectedSource != null) {
                result = result.filter { it.source == selectedSource }
            }
            if (query.isNotBlank()) {
                val q = query.lowercase()
                result = result.filter { flag ->
                    flag.key.lowercase().contains(q) ||
                        flag.value.lowercase().contains(q) ||
                        flag.source?.lowercase()?.contains(q) == true ||
                        flag.type?.lowercase()?.contains(q) == true
                }
            }
            return result
        }
}

class FeatureFlagViewModel(
    repository: DevToolsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _query = MutableStateFlow("")
    private val _selectedSource = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FeatureFlagUiState> =
        combine(repository.featureFlags, _query, _selectedSource) { flags, query, source ->
            FeatureFlagUiState(
                flags = flags,
                query = query,
                selectedSource = source,
                totalCount = flags.size,
                sources = flags.mapNotNull { it.source }.distinct().sorted(),
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000L), FeatureFlagUiState())

    fun onQueryChange(query: String) {
        _query.value = query
    }

    fun onSourceSelected(source: String?) {
        _selectedSource.value = if (_selectedSource.value == source) null else source
    }

    fun close() {
        scope.cancel()
    }
}
