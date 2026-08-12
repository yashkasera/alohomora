package io.github.yashkasera.alohomora.desktop.app

import java.util.prefs.Preferences

internal object DesktopThemePrefs {
    private val prefs = Preferences.userRoot().node("io/github/yashkasera/alohomora/desktop")
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_THEME_ID = "theme_id"
    private const val KEY_DARK_MODE_LEGACY = "dark_mode"

    fun loadMode(): ThemeMode {
        val stored = prefs.get(KEY_THEME_MODE, null)
        if (stored != null) {
            return ThemeMode.entries.firstOrNull { it.name == stored } ?: ThemeMode.SYSTEM
        }
        if (prefs.get(KEY_DARK_MODE_LEGACY, null) != null) {
            val legacy = prefs.getBoolean(KEY_DARK_MODE_LEGACY, true)
            val mode = if (legacy) ThemeMode.DARK else ThemeMode.LIGHT
            saveMode(mode)
            prefs.remove(KEY_DARK_MODE_LEGACY)
            return mode
        }
        return ThemeMode.SYSTEM
    }

    fun saveMode(mode: ThemeMode) = prefs.put(KEY_THEME_MODE, mode.name)

    fun loadThemeId(): String = prefs.get(KEY_THEME_ID, "default")

    fun saveThemeId(id: String) = prefs.put(KEY_THEME_ID, id)

    fun clear() {
        prefs.remove(KEY_THEME_MODE)
        prefs.remove(KEY_THEME_ID)
        prefs.remove(KEY_DARK_MODE_LEGACY)
    }
}
