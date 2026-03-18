package io.github.yashkasera.alohomora.devtools

import androidx.compose.runtime.Immutable

@Immutable
internal data class DevToolsServerState(
    val isRunning: Boolean = false,
    val port: Int? = null,
    val hasClient: Boolean = false,
    val lastError: String? = null,
    val pendingOtp: String? = null,
)
