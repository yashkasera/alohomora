package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BuildMetadataStore {
    private val _buildInfo = MutableStateFlow<BuildInfo?>(null)
    val buildInfo: StateFlow<BuildInfo?> = _buildInfo.asStateFlow()

    fun replace(buildInfo: BuildInfo?) {
        _buildInfo.value = buildInfo
    }

    fun clear() {
        _buildInfo.value = null
    }
}
