package io.github.yashkasera.alohomora.desktop.data.devtools

import java.util.prefs.Preferences

/**
 * Persistence for the read-only MCP server toggle and port, mirroring [DesktopThemePrefs] /
 * [DesktopEventPrefs].
 *
 * Off by default: the server is the desktop app's only inbound listener, so enabling it is an
 * explicit opt-in. The default port avoids DevTools' own 53999.
 */
internal object DesktopMcpPrefs {
    private val prefs = Preferences.userRoot().node("io/github/yashkasera/alohomora/desktop/mcp")

    private const val KEY_ENABLED = "enabled"
    private const val KEY_PORT = "port"
    private const val KEY_WRITE_ENABLED = "write_enabled"

    const val DEFAULT_PORT = 53900

    fun loadEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun saveEnabled(enabled: Boolean) = prefs.putBoolean(KEY_ENABLED, enabled)

    fun loadPort(): Int = prefs.getInt(KEY_PORT, DEFAULT_PORT)

    fun savePort(port: Int) = prefs.putInt(KEY_PORT, port)

    /** Whether the agent may run write/command tools. Off by default — a second, explicit opt-in. */
    fun loadWriteEnabled(): Boolean = prefs.getBoolean(KEY_WRITE_ENABLED, false)

    fun saveWriteEnabled(enabled: Boolean) = prefs.putBoolean(KEY_WRITE_ENABLED, enabled)

    fun clear() {
        prefs.remove(KEY_ENABLED)
        prefs.remove(KEY_PORT)
        prefs.remove(KEY_WRITE_ENABLED)
    }
}
