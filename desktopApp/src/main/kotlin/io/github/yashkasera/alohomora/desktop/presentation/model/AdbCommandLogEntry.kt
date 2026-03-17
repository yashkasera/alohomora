package io.github.yashkasera.alohomora.desktop.presentation.model

data class AdbCommandLogEntry(
    val timestamp: Long,
    val deviceId: String?,
    val command: String,
)
