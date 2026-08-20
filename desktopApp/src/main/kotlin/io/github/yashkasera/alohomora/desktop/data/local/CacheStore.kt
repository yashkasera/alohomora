package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.CacheStoreSnapshot
import io.github.yashkasera.alohomora.desktop.domain.model.CacheEntryState
import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
import io.github.yashkasera.alohomora.desktop.domain.model.CacheStoreState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CacheStore {
    private val _state = MutableStateFlow(CacheState())
    val state: StateFlow<CacheState> = _state.asStateFlow()

    fun replaceKeys(keys: List<String>) {
        _state.value = _state.value.copy(keys = keys.sorted())
    }

    fun applySnapshot(keys: List<String>, values: Map<String, String?>) {
        _state.value = _state.value.copy(
            keys = if (keys.isNotEmpty()) keys.sorted() else _state.value.keys,
            values = _state.value.values + values,
        )
    }

    fun applyStoreSnapshot(storeSnapshots: List<CacheStoreSnapshot>) {
        if (storeSnapshots.isEmpty()) return
        val stores = storeSnapshots.map { snapshot ->
            CacheStoreState(
                name = snapshot.name,
                isEncrypted = snapshot.isEncrypted,
                entries = snapshot.entries.map { entry ->
                    CacheEntryState(
                        key = entry.key,
                        value = entry.value,
                        type = entry.type,
                    )
                },
            )
        }
        val allKeys = stores.flatMap { s -> s.entries.map { it.key } }.sorted()
        val allValues = stores.flatMap { s -> s.entries.map { it.key to it.value } }.toMap()
        _state.value = _state.value.copy(
            stores = stores,
            keys = allKeys,
            values = allValues,
        )
    }

    fun clear() {
        _state.value = CacheState()
    }
}
