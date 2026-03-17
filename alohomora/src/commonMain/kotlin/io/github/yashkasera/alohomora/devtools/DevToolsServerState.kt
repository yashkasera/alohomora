package io.github.yashkasera.alohomora.devtools

data class DevToolsServerState(
    val isRunning: Boolean = false,
    val port: Int? = null,
    val hasClient: Boolean = false,
    val lastError: String? = null,
)
