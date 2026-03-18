package io.github.yashkasera.alohomora.desktop.app

import java.util.prefs.Preferences

internal object DesktopThemePrefs {
    private val prefs = Preferences.userRoot().node("io/github/yashkasera/alohomora/desktop")
    private const val KEY_DARK_MODE = "dark_mode"

    fun load(): Boolean = prefs.getBoolean(KEY_DARK_MODE, true)

    fun save(isDark: Boolean) = prefs.putBoolean(KEY_DARK_MODE, isDark)
}
