package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.data.config.ConfigRepoManager
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopConfigRepoPrefs
import io.github.yashkasera.alohomora.desktop.presentation.model.ConfigRepoUiState
import java.awt.Desktop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Drives the Team-config surface. Connect/Initialize are the only git-vocabulary-free entry points; the
 * developer-mode toggle governs git/repo mechanics only. [onChanged] lets the journey list refresh its
 * team view after a connect/sync so the two stay in step.
 */
class ConfigRepoViewModel(
    private val manager: ConfigRepoManager,
    private val onChanged: () -> Unit = {},
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _urlInput = MutableStateFlow(DesktopConfigRepoPrefs.loadRepoUrl().orEmpty())
    private val _patInput = MutableStateFlow("")
    private val _busy = MutableStateFlow(false)

    val uiState: StateFlow<ConfigRepoUiState> = combine(
        combine(manager.status, manager.developerMode, manager.syncStatus) { s, d, sync -> Triple(s, d, sync) },
        _urlInput,
        _patInput,
        _busy,
    ) { (status, developerMode, syncStatus), url, pat, busy ->
        ConfigRepoUiState(
            status = status,
            developerMode = developerMode,
            syncStatus = syncStatus,
            urlInput = url,
            patInput = pat,
            busy = busy,
        )
    }.stateIn(scope, SharingStarted.Eagerly, ConfigRepoUiState())

    fun onUrlChange(url: String) {
        _urlInput.value = url
    }

    fun onPatChange(pat: String) {
        _patInput.value = pat
    }

    fun connect() = run {
        val url = _urlInput.value.trim().ifBlank { return@run }
        scope.launch {
            _busy.value = true
            manager.connect(url, _patInput.value.trim().ifBlank { null })
            _patInput.value = ""
            _busy.value = false
            onChanged()
        }
    }

    fun initialize() = run {
        val url = _urlInput.value.trim().ifBlank { return@run }
        scope.launch {
            _busy.value = true
            manager.initialize(url, _patInput.value.trim().ifBlank { null })
            _patInput.value = ""
            _busy.value = false
            onChanged()
        }
    }

    fun sync() {
        scope.launch {
            _busy.value = true
            manager.sync()
            _busy.value = false
            onChanged()
        }
    }

    fun disconnect() {
        manager.disconnect()
        onChanged()
    }

    fun setDeveloperMode(enabled: Boolean) = manager.setDeveloperMode(enabled)

    /**
     * Opens the clone directory in the OS file manager — the honest fallback for "reveal in terminal",
     * since the pty4j terminal the docs describe does not exist in this codebase.
     */
    fun revealRepo() {
        val dir = manager.repoDir()
        if (dir.exists() && Desktop.isDesktopSupported()) {
            runCatching { Desktop.getDesktop().open(dir) }
        }
    }

    fun close() = scope.cancel()
}
