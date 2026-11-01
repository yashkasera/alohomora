package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.devtools.ALOHOMORA_DEFAULTS_KEY_PREFIX
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSUserDefaults

/**
 * The Cache inspector is reachable by any paired desktop, so Alohomora's own keys (desktop trust
 * tokens among them) must neither be served nor be writable through it.
 */
class CacheRepositoryInternalKeysTest {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val internalKey = "${ALOHOMORA_DEFAULTS_KEY_PREFIX}test.secret"
    private val hostKey = "alohomora_test_host_key"

    @BeforeTest
    fun setUp() {
        defaults.setObject("token-value", forKey = internalKey)
        defaults.setObject("host-value", forKey = hostKey)
    }

    @AfterTest
    fun tearDown() {
        defaults.removeObjectForKey(internalKey)
        defaults.removeObjectForKey(hostKey)
    }

    @Test
    fun `internal keys are not listed`() = runTest {
        val keys = CacheRepositoryImpl().refresh().map { it.key }

        assertFalse(internalKey in keys)
        assertTrue(hostKey in keys)
    }

    @Test
    fun `internal keys cannot be overwritten`() = runTest {
        val updated = CacheRepositoryImpl().updateEntry("NSUserDefaults", internalKey, "planted", "STRING")

        assertFalse(updated)
        assertEquals("token-value", defaults.stringForKey(internalKey))
    }

    @Test
    fun `internal keys cannot be deleted`() = runTest {
        val deleted = CacheRepositoryImpl().deleteEntry("NSUserDefaults", internalKey)

        assertFalse(deleted)
        assertEquals("token-value", defaults.stringForKey(internalKey))
    }

    @Test
    fun `host keys stay editable`() = runTest {
        assertTrue(CacheRepositoryImpl().updateEntry("NSUserDefaults", hostKey, "edited", "STRING"))
        assertEquals("edited", defaults.stringForKey(hostKey))
    }
}
