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
 *
 * No headers are redacted by default. Call `Alohomora.redactHeaders(...)` to specify which
 * headers to redact.
 */
object HeaderRedaction {

    const val REDACTED = "[REDACTED]"

    /**
     * Header names to redact, lowercased for case-insensitive matching.
     *
     * Set via `Alohomora.redactHeaders(...)`. Empty by default — nothing is redacted until
     * the developer opts in.
     */
    var headersToRedact: Set<String> = emptySet()
        private set

    fun setHeaders(headers: Set<String>) {
        headersToRedact = headers.mapTo(HashSet()) { it.lowercase() }
    }

    fun clearHeaders() {
        headersToRedact = emptySet()
    }

    fun isSensitive(name: String): Boolean =
        headersToRedact.isNotEmpty() && name.lowercase() in headersToRedact

    /** Returns [headers] with the values of every sensitive header replaced by [REDACTED]. */
    fun redact(headers: Map<String, List<String>>?): Map<String, List<String>>? {
        if (headers == null) return null
        if (headersToRedact.isEmpty()) return headers
        if (headers.none { isSensitive(it.key) }) return headers
        return headers.mapValues { (name, values) ->
            if (isSensitive(name)) values.map { REDACTED } else values
        }
    }
}
