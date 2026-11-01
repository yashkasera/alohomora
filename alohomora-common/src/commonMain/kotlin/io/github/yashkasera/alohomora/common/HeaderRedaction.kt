package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.common.HeaderRedaction.REDACTED
import kotlin.concurrent.Volatile

/**
 * Redaction policy for captured HTTP headers.
 *
 * Captured headers do not stay in the app: they are written to SQLite, streamed to the
 * desktop client, interpolated into shareable `curl` commands, posted to Slack, and served to
 * agents over the desktop's MCP server. Storing them verbatim meant every bearer token, session
 * cookie and API key in the host app's traffic followed all of those paths.
 *
 * Redaction is applied at capture time, not at render time, so a secret never reaches the
 * database in the first place — clearing the UI or rotating the desktop client cannot
 * retroactively leak what was never stored.
 *
 * The effective set is [DEFAULT_HEADERS] (unless turned off with
 * `Alohomora.setDefaultHeaderRedactionEnabled(false)`) plus whatever the app named via
 * `Alohomora.redactHeaders(...)`. The defaults are on because the failure mode was asymmetric:
 * an app that forgot to opt in leaked credentials to every one of those sinks, while an app that
 * genuinely needs a raw token in the console loses nothing by saying so once.
 */
object HeaderRedaction {

    const val REDACTED = "[REDACTED]"

    /**
     * Headers redacted out of the box, lowercased.
     *
     * Limited to names whose value is a credential in every app that sends them. App-specific
     * names such as a payload signature are left to `Alohomora.redactHeaders(...)`: a default list
     * that guessed at those would redact debugging data in apps where the name means something
     * else, and still miss the header the app actually uses.
     */
    val DEFAULT_HEADERS: Set<String> = setOf(
        "authorization",
        "proxy-authorization",
        "cookie",
        "set-cookie",
        "x-api-key",
    )

    /**
     * Both inputs and the set derived from them, swapped as one value.
     *
     * Kept together so a capture thread can never pair the new defaults flag with the old custom
     * set (or the reverse) and redact against a set that was never configured.
     */
    private class Policy(val defaultsEnabled: Boolean, val custom: Set<String>) {
        val effective: Set<String> = if (defaultsEnabled) DEFAULT_HEADERS + custom else custom
    }

    /**
     * `@Volatile`: written from the configuring thread (usually app startup) but read from
     * network interceptor threads at capture time. Without the memory barrier an interceptor
     * could observe a stale policy and persist a secret unredacted — the one registry here
     * where the missing barrier has a security consequence, so it matches its `@Volatile`
     * siblings ([NetworkRuleEngine], the capability registries).
     */
    @Volatile
    private var policy = Policy(defaultsEnabled = true, custom = emptySet())

    /** Every header name currently redacted, lowercased: the defaults (if on) plus the custom set. */
    val headersToRedact: Set<String> get() = policy.effective

    /** Whether [DEFAULT_HEADERS] are part of [headersToRedact]. True unless the app opted out. */
    val defaultsEnabled: Boolean get() = policy.defaultsEnabled

    /** Replaces the app's own header names. [DEFAULT_HEADERS] are unaffected. */
    fun setHeaders(headers: Set<String>) {
        policy = Policy(policy.defaultsEnabled, headers.mapTo(HashSet()) { it.lowercase() })
    }

    /** Clears the app's own header names. [DEFAULT_HEADERS] stay on unless separately disabled. */
    fun clearHeaders() {
        policy = Policy(policy.defaultsEnabled, emptySet())
    }

    /** Turns [DEFAULT_HEADERS] on or off. The app's own names are redacted either way. */
    fun setDefaultsEnabled(enabled: Boolean) {
        policy = Policy(enabled, policy.custom)
    }

    fun isSensitive(name: String): Boolean = name.lowercase() in headersToRedact

    /**
     * Returns [headers] with the values of every sensitive header replaced by [REDACTED].
     *
     * Reads the policy once, so a reconfiguration landing mid-call cannot redact half of one
     * request's headers against the old set and half against the new.
     */
    fun redact(headers: Map<String, List<String>>?): Map<String, List<String>>? {
        if (headers == null) return null
        val effective = headersToRedact
        if (effective.isEmpty()) return headers
        if (headers.keys.none { it.lowercase() in effective }) return headers
        return headers.mapValues { (name, values) ->
            if (name.lowercase() in effective) values.map { REDACTED } else values
        }
    }
}
