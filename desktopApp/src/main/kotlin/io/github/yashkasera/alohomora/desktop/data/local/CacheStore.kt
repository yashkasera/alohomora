package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
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

    fun clear() {
        _state.value = CacheState()
    }
}
