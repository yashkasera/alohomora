package io.github.yashkasera.alohomora.devtools

import androidx.compose.runtime.Immutable

@Immutable
internal data class DevToolsServerState(
    val isRunning: Boolean = false,
    val port: Int? = null,
    val hasClient: Boolean = false,
    val lastError: String? = null,
    val pendingOtp: String? = null,
    /**
     * Whether the user has agreed to remember the desktop currently being paired.
     *
     * Defaults to false and resets on every new connection: persisting a credential is an
     * explicit choice, not something inferred from a successful pairing. Only consulted when an
     * OTP is accepted — a client authenticating with an existing token changes nothing.
     */
    val rememberDevice: Boolean = false,
)
