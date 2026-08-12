package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.FeatureFlag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class FeatureFlagStore {
    private val _flags = MutableStateFlow<Map<String, FeatureFlag>>(emptyMap())
    val flags: StateFlow<Map<String, FeatureFlag>> = _flags.asStateFlow()

    fun put(flag: FeatureFlag) {
        _flags.update { it + (flag.key to flag) }
    }

    fun putAll(flags: List<FeatureFlag>, source: String?) {
        _flags.update { current ->
            if (source == null) {
                flags.associateBy { it.key }
            } else {
                current.filterValues { it.source != source } + flags.associateBy { it.key }
            }
        }
    }

    fun getAll(): List<FeatureFlag> = _flags.value.values.toList()

    fun clear() {
        _flags.value = emptyMap()
    }
}
