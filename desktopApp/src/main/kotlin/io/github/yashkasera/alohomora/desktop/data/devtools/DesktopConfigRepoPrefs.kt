package io.github.yashkasera.alohomora.desktop.data.devtools

import java.io.File
import java.util.prefs.Preferences

/**
 * Per-user config-repo settings, persisted like [DesktopMcpPrefs] (`java.util.prefs`). Nothing is baked
 * into the binary — repo URL and clone path are runtime, so the OSS build ships forge-neutral.
 *
 * The PAT is deliberately **not** here: `java.util.prefs` is effectively plaintext, so credentials go
 * in the OS keychain (added with the connect UI). This holds only non-secret settings.
 */
object DesktopConfigRepoPrefs {
    private val prefs = Preferences.userRoot()
        .node("io/github/yashkasera/alohomora/desktop/config")

    private const val KEY_REPO_URL = "repo_url"
    private const val KEY_CLONE_PATH = "clone_path"
    private const val KEY_DEVELOPER_MODE = "developer_mode"

    fun defaultClonePath(): String = File(System.getProperty("user.home"), ".alohomora/config-repo").path

    fun loadRepoUrl(): String? = prefs.get(KEY_REPO_URL, null)?.ifBlank { null }
    fun saveRepoUrl(url: String?) {
        if (url.isNullOrBlank()) prefs.remove(KEY_REPO_URL) else prefs.put(KEY_REPO_URL, url)
    }

    fun loadClonePath(): String = prefs.get(KEY_CLONE_PATH, defaultClonePath())
    fun saveClonePath(path: String) = prefs.put(KEY_CLONE_PATH, path)

    /** Governs git/repo mechanics only; off by default. Global to the clone, not per-journey. */
    fun loadDeveloperMode(): Boolean = prefs.getBoolean(KEY_DEVELOPER_MODE, false)
    fun saveDeveloperMode(enabled: Boolean) = prefs.putBoolean(KEY_DEVELOPER_MODE, enabled)
}
