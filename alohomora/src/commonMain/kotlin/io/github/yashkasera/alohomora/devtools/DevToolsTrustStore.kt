package io.github.yashkasera.alohomora.devtools

/**
 * Remembers desktop clients that have already been approved with an OTP.
 *
 * Trust-on-first-use: the user types the code once per machine, the device issues a token, and
 * that machine reconnects silently from then on. Without this the OTP prompt fires on every
 * reconnect — many times an hour during a debug session — which trains people to dismiss it and
 * defeats the point of having it.
 *
 * Tokens are long-lived credentials, so [issue] must use a cryptographically secure source. A
 * predictable token would be a permanent authentication bypass, which is strictly worse than the
 * 4-digit OTP it replaces.
 */
internal interface DevToolsTrustStore {

    /** True if [token] was issued by this device and has not been revoked. */
    fun isTrusted(token: String): Boolean

    /** Creates, persists and returns a new token. */
    fun issue(): String

    /** Forgets every issued token, forcing all clients back through the OTP prompt. */
    fun revokeAll()
}

/** Bytes of entropy per token. 256 bits — brute force is not a consideration at this size. */
internal const val TRUST_TOKEN_BYTES = 32

/** Cap on remembered tokens, so repeated pairings cannot grow storage without bound. */
internal const val MAX_TRUSTED_TOKENS = 16

/**
 * The token to return with AUTH_SUCCESS after an OTP is accepted, or null to keep the pairing
 * one-off.
 *
 * A free function rather than an inline `if` inside the connection handler so the rule can be
 * tested directly. Getting it backwards persists a credential the user explicitly declined,
 * which is the whole failure mode the consent checkbox exists to prevent — and it would be
 * invisible until someone noticed they were never prompted again.
 */
internal fun DevToolsTrustStore.tokenForAcceptedOtp(rememberDevice: Boolean): String? =
    if (rememberDevice) issue() else null
