package io.github.yashkasera.alohomora.devtools

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSUserDefaults
import platform.posix.arc4random_buf

/**
 * Prefix for every key Alohomora writes into the host app's standard NSUserDefaults. The Cache
 * inspector hides and refuses to edit anything under it.
 */
internal const val ALOHOMORA_DEFAULTS_KEY_PREFIX: String = "alohomora."

/**
 * [DevToolsTrustStore] backed by NSUserDefaults.
 *
 * Namespaced under [ALOHOMORA_DEFAULTS_KEY_PREFIX] so it cannot collide with host-app keys, and so
 * the Cache inspector can exclude it.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosTrustStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : DevToolsTrustStore {

    override fun isTrusted(token: String): Boolean =
        token.isNotEmpty() && token in trustedTokens()

    override fun issue(): String {
        // arc4random_buf is Darwin's CSPRNG. kotlin.random.Random would not do: a token is a
        // long-lived credential replacing the OTP, so predictability is a permanent bypass.
        val bytes = ByteArray(TRUST_TOKEN_BYTES)
        bytes.usePinned { pinned ->
            arc4random_buf(pinned.addressOf(0), TRUST_TOKEN_BYTES.toULong())
        }
        val token = bytes.joinToString("") { byte ->
            (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
        }

        val retained = (listOf(token) + trustedTokens()).take(MAX_TRUSTED_TOKENS)
        defaults.setObject(retained, forKey = KEY_TOKENS)
        return token
    }

    override fun revokeAll() {
        defaults.removeObjectForKey(KEY_TOKENS)
    }

    private fun trustedTokens(): List<String> =
        defaults.stringArrayForKey(KEY_TOKENS)?.filterIsInstance<String>().orEmpty()

    private companion object {
        const val KEY_TOKENS = "${ALOHOMORA_DEFAULTS_KEY_PREFIX}devtools.trusted_tokens"
    }
}
