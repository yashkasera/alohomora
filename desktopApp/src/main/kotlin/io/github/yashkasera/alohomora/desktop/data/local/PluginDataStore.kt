package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.PluginDataSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PluginDataStore {
    private val _snapshots = MutableStateFlow<List<PluginDataSnapshot>>(emptyList())
    val snapshots: StateFlow<List<PluginDataSnapshot>> = _snapshots.asStateFlow()

    fun replace(snapshots: List<PluginDataSnapshot>) {
        _snapshots.value = snapshots.sortedBy { it.pluginId }
    }

    fun mergeSnapshot(snapshot: PluginDataSnapshot) {
        _snapshots.value = _snapshots.value
            .filter { it.pluginId != snapshot.pluginId }
            .plus(snapshot)
            .sortedBy { it.pluginId }
    }

    fun clear() {
        _snapshots.value = emptyList()
    }
}
