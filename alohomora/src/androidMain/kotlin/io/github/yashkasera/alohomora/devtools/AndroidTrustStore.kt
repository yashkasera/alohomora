package io.github.yashkasera.alohomora.devtools

import android.content.Context
import androidx.core.content.edit
import java.security.SecureRandom

/**
 * [DevToolsTrustStore] backed by SharedPreferences.
 *
 * Stored in a dedicated file rather than the app's own preferences so it cannot collide with
 * host-app keys, and so the Preferences inspector shows it as clearly Alohomora's.
 */
internal class AndroidTrustStore(context: Context) : DevToolsTrustStore {

    private val prefs = context.applicationContext
        .getSharedPreferences("alohomora_devtools_trust", Context.MODE_PRIVATE)

    // SecureRandom, not kotlin.random.Random: a token is a long-lived credential that replaces
    // the OTP entirely, so a predictable one would be a permanent auth bypass.
    private val random = SecureRandom()

    override fun isTrusted(token: String): Boolean =
        token.isNotEmpty() && token in trustedTokens()

    override fun issue(): String {
        val token = ByteArray(TRUST_TOKEN_BYTES)
            .also(random::nextBytes)
            .joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }

        // Keep the most recent N. Each desktop pairing mints one, so an unbounded set would grow
        // forever on a machine that is reinstalled repeatedly.
        val retained = (listOf(token) + trustedTokens()).take(MAX_TRUSTED_TOKENS)
        prefs.edit { putStringSet(KEY_TOKENS, retained.toSet()) }
        return token
    }

    override fun revokeAll() {
        prefs.edit { remove(KEY_TOKENS) }
    }

    private fun trustedTokens(): Set<String> =
        prefs.getStringSet(KEY_TOKENS, emptySet()).orEmpty()

    private companion object {
        const val KEY_TOKENS = "trusted_tokens"
    }
}
