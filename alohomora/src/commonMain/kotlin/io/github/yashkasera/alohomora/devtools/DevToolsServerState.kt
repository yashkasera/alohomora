package io.github.yashkasera.alohomora.devtools

internal data class DevToolsServerState(
    val isRunning: Boolean = false,
    val port: Int? = null,
    val hasClient: Boolean = false,
    val lastError: String? = null,
)
