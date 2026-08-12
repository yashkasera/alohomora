package io.github.yashkasera.alohomora.desktop.data.devtools

import java.util.prefs.Preferences

/**
 * Trust tokens issued by devices this desktop has already paired with, keyed by device id.
 *
 * Presenting a stored token lets an approved machine reconnect without the user reading a code
 * off the phone every time. Keyed per device because a token is only meaningful to the device
 * that minted it.
 *
 * Uses [Preferences] to match [io.github.yashkasera.alohomora.desktop.app.DesktopThemePrefs] —
 * the tokens are debug-session credentials for a loopback service, not secrets worth pulling in a
 * keychain dependency for.
 */
internal object DesktopTrustPrefs {

    private val prefs = Preferences.userRoot()
        .node("io/github/yashkasera/alohomora/desktop/trust")

    fun tokenFor(deviceId: String?): String? =
        deviceId?.takeIf { it.isNotBlank() }?.let { prefs.get(it.key(), null) }

    fun save(deviceId: String?, token: String) {
        deviceId?.takeIf { it.isNotBlank() }?.let { prefs.put(it.key(), token) }
    }

    /**
     * Drops the stored token so the next connect falls back to the OTP prompt.
     *
     * Called on auth failure: if the device has revoked its tokens (app data cleared,
     * reinstalled), a stale token would otherwise fail forever with no way back.
     */
    fun forget(deviceId: String?) {
        deviceId?.takeIf { it.isNotBlank() }?.let { prefs.remove(it.key()) }
    }

    /**
     * Preferences keys are capped at [Preferences.MAX_KEY_LENGTH] (80 chars) and a long device id
     * would throw on put. Hash anything oversized rather than truncating, which could collide two
     * devices sharing a prefix.
     */
    private fun String.key(): String =
        if (length <= MAX_KEY_LENGTH) this else "h${hashCode()}"

    fun clearAll() { prefs.clear() }

    private const val MAX_KEY_LENGTH = 80
}
