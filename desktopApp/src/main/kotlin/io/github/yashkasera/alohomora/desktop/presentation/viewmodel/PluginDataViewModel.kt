package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.PluginDataSnapshot
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PluginDataUiState(
    val snapshots: List<PluginDataSnapshot> = emptyList(),
) {
    val isEmpty: Boolean get() = snapshots.isEmpty()
}

class PluginDataViewModel(
    private val repository: DevToolsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val uiState: StateFlow<PluginDataUiState> =
        repository.pluginData
            .map { PluginDataUiState(snapshots = it) }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000L), PluginDataUiState())

    fun updateField(pluginId: String, key: String, value: String) {
        repository.requestPluginDataUpdate(pluginId, key, value)
    }

    fun close() {
        scope.cancel()
    }
}
