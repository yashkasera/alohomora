package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.data.config.ConfigRepoStatus
import io.github.yashkasera.alohomora.desktop.domain.config.SyncStatus

/** Immutable state for the Team-config settings surface and the journey list's team affordances. */
data class ConfigRepoUiState(
    val status: ConfigRepoStatus = ConfigRepoStatus.Disconnected,
    val developerMode: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.NotConnected,
    val urlInput: String = "",
    /** HTTPS personal access token input; saved to the OS keychain on connect, never displayed back. */
    val patInput: String = "",
    /** A connect/initialize/sync operation is in flight. */
    val busy: Boolean = false,
) {
    val isConnected: Boolean get() = status is ConfigRepoStatus.Connected
}
