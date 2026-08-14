package io.github.yashkasera.alohomora.desktop.data.devtools

import java.util.prefs.Preferences

internal object DesktopScreenshotPrefs {
    private val prefs = Preferences.userRoot()
        .node("io/github/yashkasera/alohomora/desktop/screenshot")

    private const val KEY_DEFAULT_DIR = "default_dir"
    private const val KEY_SHOW_TOAST = "show_toast"

    fun loadDefaultDir(): String = prefs.get(KEY_DEFAULT_DIR, "")

    fun saveDefaultDir(dir: String) = prefs.put(KEY_DEFAULT_DIR, dir)

    fun loadShowToast(): Boolean = prefs.getBoolean(KEY_SHOW_TOAST, true)

    fun saveShowToast(show: Boolean) = prefs.putBoolean(KEY_SHOW_TOAST, show)

    fun clear() {
        prefs.remove(KEY_DEFAULT_DIR)
        prefs.remove(KEY_SHOW_TOAST)
    }
}
