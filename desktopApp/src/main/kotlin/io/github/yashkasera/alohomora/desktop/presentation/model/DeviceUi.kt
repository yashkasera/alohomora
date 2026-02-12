package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState

data class DeviceUi(
    val id: String,
    val state: DeviceState,
    val model: String? = null,
)
