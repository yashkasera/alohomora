package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.FeatureFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FeatureFlagStore {
    private val _flags = MutableStateFlow<List<FeatureFlag>>(emptyList())
    val flags: StateFlow<List<FeatureFlag>> = _flags.asStateFlow()

    fun replace(flags: List<FeatureFlag>) {
        _flags.value = flags.sortedBy { it.key }
    }

    fun clear() {
        _flags.value = emptyList()
    }
}
