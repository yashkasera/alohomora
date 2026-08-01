package io.github.yashkasera.alohomora.devtools

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.Foundation.NSUserDefaults

/**
 * Trust tokens replace the OTP entirely for an approved desktop, so a weakness here is a
 * permanent authentication bypass rather than a one-shot guess. These pin the properties that
 * make that trade safe.
 */
class IosTrustStoreTest {

    private lateinit var defaults: NSUserDefaults
    private lateinit var store: IosTrustStore

    @BeforeTest
    fun setUp() {
        // A private suite, so the test never reads or clobbers the real app's defaults.
        defaults = NSUserDefaults(suiteName = SUITE)
        store = IosTrustStore(defaults)
        store.revokeAll()
    }

    @AfterTest
    fun tearDown() {
        store.revokeAll()
        NSUserDefaults.standardUserDefaults.removePersistentDomainForName(SUITE)
    }

    @Test
    fun `an issued token is trusted`() {
        val token = store.issue()

        assertTrue(store.isTrusted(token))
    }

    @Test
    fun `an unknown token is rejected`() {
        store.issue()

        assertFalse(store.isTrusted("not-a-real-token"))
    }

    @Test
    fun `the empty string is never trusted`() {
        // A client that omits the token sends null, which the desktop may render as "". Treating
        // that as valid would let anything connect without a code at all.
        store.issue()

        assertFalse(store.isTrusted(""))
    }

    @Test
    fun `tokens are long and unpredictable`() {
        val tokens = List(16) { store.issue() }

        assertEquals(16, tokens.toSet().size, "issued tokens must never repeat")
        tokens.forEach { token ->
            assertEquals(
                TRUST_TOKEN_BYTES * 2,
                token.length,
                "expected ${TRUST_TOKEN_BYTES * 2} hex chars of entropy",
            )
            assertTrue(
                token.all { it in "0123456789abcdef" },
                "token should be lowercase hex, was '$token'",
            )
        }
    }

    @Test
    fun `several desktops can be trusted at once`() {
        val first = store.issue()
        val second = store.issue()

        assertTrue(store.isTrusted(first), "issuing for a second desktop revoked the first")
        assertTrue(store.isTrusted(second))
    }

    @Test
    fun `the oldest token is evicted past the cap`() {
        val oldest = store.issue()
        repeat(MAX_TRUSTED_TOKENS) { store.issue() }

        assertFalse(store.isTrusted(oldest), "storage would otherwise grow without bound")
    }

    @Test
    fun `revokeAll forces every client back to the otp`() {
        val token = store.issue()
        store.revokeAll()

        assertFalse(store.isTrusted(token))
    }

    @Test
    fun `trust survives a new store instance`() {
        // The whole point is skipping the prompt after an app restart, which means reading back
        // from persistent storage rather than an in-memory set.
        val token = store.issue()

        assertTrue(IosTrustStore(NSUserDefaults(suiteName = SUITE)).isTrusted(token))
    }

    private companion object {
        const val SUITE = "io.github.yashkasera.alohomora.trust.test"
    }
}
