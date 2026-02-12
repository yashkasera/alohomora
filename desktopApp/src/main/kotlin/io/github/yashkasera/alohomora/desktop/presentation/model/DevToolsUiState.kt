package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection

data class DevToolsUiState(
    val connection: DevToolsConnection,
    val currentDeviceId: String?,
    val switching: Boolean,
)
