package io.github.yashkasera.alohomora.desktop.domain.model

data class Device(
    val id: String,
    val state: DeviceState,
    val model: String? = null,
    val product: String? = null,
    val transportId: String? = null,
)
