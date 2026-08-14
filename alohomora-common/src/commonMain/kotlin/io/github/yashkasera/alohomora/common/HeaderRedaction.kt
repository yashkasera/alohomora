package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.common.HeaderRedaction.REDACTED

/**
 * Redaction policy for captured HTTP headers.
 *
 * Captured headers do not stay in the app: they are written to SQLite, streamed to the
 * desktop client, interpolated into shareable `curl` commands, and posted to Slack. Storing
 * them verbatim meant every bearer token, session cookie and API key in the host app's
 * traffic followed all of those paths.
 *
 * Redaction is applied at capture time, not at render time, so a secret never reaches the
 * database in the first place — clearing the UI or rotating the desktop client cannot
 * retroactively leak what was never stored.
 */
object HeaderRedaction {

    const val REDACTED = "[REDACTED]"

    /**
     * Header names redacted by default. Matched case-insensitively, since HTTP header names
     * are case-insensitive and real traffic mixes `authorization` and `Authorization`.
     */
    val DEFAULT_DENYLIST: Set<String> = setOf(
        "authorization",
        "proxy-authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
        "x-auth-token",
        "x-csrf-token",
    )

    /**
     * Additional header names to redact, contributed by the host app.
     *
     * Assign at startup; every app has its own bespoke auth headers that no built-in list can
     * anticipate.
     */
    var additionalDenylist: Set<String> = emptySet()

    fun isSensitive(name: String): Boolean {
        val lower = name.lowercase()
        return lower in DEFAULT_DENYLIST || additionalDenylist.any {
            it.equals(
                name,
                ignoreCase = true,
            )
        }
    }

    /** Returns [headers] with the values of every sensitive header replaced by [REDACTED]. */
    fun redact(headers: Map<String, List<String>>?): Map<String, List<String>>? {
        if (headers == null) return null
        if (headers.none { isSensitive(it.key) }) return headers
        return headers.mapValues { (name, values) ->
            if (isSensitive(name)) values.map { REDACTED } else values
        }
    }
}
