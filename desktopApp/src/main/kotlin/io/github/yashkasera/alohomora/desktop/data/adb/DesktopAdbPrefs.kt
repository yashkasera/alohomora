package io.github.yashkasera.alohomora.desktop.data.adb

import java.util.prefs.Preferences

/**
 * Persists a user-supplied path to the `adb` executable, for machines where [AdbLocator]'s
 * automatic resolution (ANDROID_HOME, conventional SDK locations, PATH) comes up empty — most
 * often a packaged app launched from Finder/Start with no shell PATH.
 *
 * The value may point at the `adb` binary itself, its `platform-tools` directory, or an SDK
 * root; [AdbLocator] accepts any of the three.
 */
internal object DesktopAdbPrefs {
    private val prefs = Preferences.userRoot().node("io/github/yashkasera/alohomora/desktop/adb")
    private const val KEY_PATH = "adb_path"

    fun loadPath(): String = prefs.get(KEY_PATH, "")

    fun savePath(path: String) {
        if (path.isBlank()) prefs.remove(KEY_PATH) else prefs.put(KEY_PATH, path.trim())
    }

    fun clear() = prefs.remove(KEY_PATH)
}
