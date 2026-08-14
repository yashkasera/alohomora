package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Collapsing the wire's two collections into one row per key.
 *
 * `CacheState` keeps keys and values apart because that is how they arrive — every key in the initial
 * snapshot, each value in its own later frame. These pin the join, and in particular the difference
 * between "not asked yet" and "asked, and the device holds nothing", which the panel used to render
 * identically as the word "null".
 */
class CacheRowsTest {

    private val state = CacheState(
        keys = listOf("auth.token", "user.name", "theme.dark"),
        values = mapOf("auth.token" to "abc123", "theme.dark" to null),
    )

    @Test
    fun `every key gets a row whether or not its value has arrived`() {
        val rows = state.toCacheRows(query = "")

        assertEquals(listOf("auth.token", "user.name", "theme.dark"), rows.map { it.key })
    }

    @Test
    fun `a key absent from the values map is pending`() {
        val row = state.toCacheRows("").single { it.key == "user.name" }

        assertTrue(row.isPending)
        assertFalse(row.isLoaded)
        assertFalse(
            row.isAbsent,
            "a value that has not arrived is not the same as one the device lacks",
        )
    }

    @Test
    fun `a key mapped to null is loaded and absent rather than pending`() {
        // The distinction the old panel could not make: both cases printed "null".
        val row = state.toCacheRows("").single { it.key == "theme.dark" }

        assertTrue(row.isLoaded)
        assertTrue(row.isAbsent)
        assertFalse(row.isPending)
    }

    @Test
    fun `a loaded value is carried through`() {
        val row = state.toCacheRows("").single { it.key == "auth.token" }

        assertEquals("abc123", row.value)
        assertTrue(row.isLoaded)
        assertFalse(row.isAbsent)
    }

    @Test
    fun `a query matches a key case-insensitively`() {
        assertEquals(listOf("auth.token"), state.toCacheRows("AUTH").map { it.key })
    }

    @Test
    fun `a query matches inside a loaded value`() {
        assertEquals(listOf("auth.token"), state.toCacheRows("abc12").map { it.key })
    }

    @Test
    fun `a query is trimmed before matching`() {
        assertEquals(listOf("auth.token"), state.toCacheRows("  auth.token  ").map { it.key })
    }

    @Test
    fun `a pending value cannot be matched by a query`() {
        // Not a bug but a limitation, and the reason cacheSubtitle reports the pending count: a value the
        // device has not sent yet is not searchable and "no match" must not read as final.
        val pending = CacheState(keys = listOf("user.name"), values = emptyMap())

        assertTrue(pending.toCacheRows("anything").isEmpty())
    }

    @Test
    fun `a blank query keeps every row`() {
        assertEquals(3, state.toCacheRows("").size)
    }

    @Test
    fun `an empty state produces no rows`() {
        assertTrue(CacheState().toCacheRows("").isEmpty())
    }
}
