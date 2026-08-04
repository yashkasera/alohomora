package io.github.yashkasera.alohomora.desktop.data.devtools

import java.util.prefs.Preferences

/**
 * Muted event names, per device.
 *
 * Per device rather than global because event names are one app's vocabulary: a set muted while
 * debugging one app would silently hide rows in an unrelated one, and an Events panel that opens empty
 * is indistinguishable from a dead connection. Caveat worth knowing — the device id identifies the
 * *device*, not the app, so two apps on one phone share a set. Keying on the package name would be more
 * accurate, but `buildInfo` only arrives after connect, so the key would change mid-session and a mute
 * made early would be written under the wrong one.
 *
 * **Only the muted set is stored.** The query, the time window and the Mark floor are deliberately
 * not: a filter restored from a previous session that happens to hide everything reads as a broken
 * stream, and a floor pinned against another session's clock can sit above the whole list.
 *
 * Uses [Preferences] to match [DesktopTrustPrefs] and
 * [io.github.yashkasera.alohomora.desktop.app.DesktopThemePrefs] — this is a UI preference, not
 * something worth a storage dependency.
 */
internal object DesktopEventPrefs {

    private val prefs = Preferences.userRoot()
        .node("io/github/yashkasera/alohomora/desktop/events")

    fun mutedNames(deviceId: String?): Set<String> =
        deviceId?.takeIf { it.isNotBlank() }
            ?.let { prefs.get(it.mutedKey(), null) }
            ?.lineSequence()
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()

    fun saveMutedNames(deviceId: String?, names: Set<String>) {
        val key = deviceId?.takeIf { it.isNotBlank() }?.mutedKey() ?: return
        if (names.isEmpty()) {
            // Removed rather than stored empty, so an unmute-all leaves nothing behind to read back.
            prefs.remove(key)
            return
        }
        prefs.put(key, names.joinToTruncated())
    }

    /**
     * Newline-joined, because an event name cannot contain one.
     *
     * Truncated rather than allowed to throw: [Preferences.put] rejects a value past
     * [Preferences.MAX_VALUE_LENGTH], and losing the whole set to an exception is worse than losing the
     * tail of an implausibly large one. Sorted so the stored value is stable across sessions.
     */
    private fun Set<String>.joinToTruncated(): String = buildString {
        for (name in this@joinToTruncated.sorted()) {
            val addition = if (isEmpty()) name else "\n$name"
            if (length + addition.length > Preferences.MAX_VALUE_LENGTH) break
            append(addition)
        }
    }

    /**
     * Keys are capped at [Preferences.MAX_KEY_LENGTH] and an oversized one would throw on put. Hashes
     * rather than truncates, which could collide two devices sharing a prefix — the same guard
     * [DesktopTrustPrefs] applies, but measured against the key *including* its suffix.
     */
    private fun String.mutedKey(): String {
        val base = if (length + KEY_SUFFIX.length <= Preferences.MAX_KEY_LENGTH) this else "h${hashCode()}"
        return base + KEY_SUFFIX
    }

    private const val KEY_SUFFIX = ".mutedEvents"
}
