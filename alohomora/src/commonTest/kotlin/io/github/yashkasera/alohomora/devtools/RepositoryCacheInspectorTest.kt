package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheSource
import io.github.yashkasera.alohomora.domain.model.CacheStore
import io.github.yashkasera.alohomora.domain.model.CacheType
import io.github.yashkasera.alohomora.domain.repository.CacheRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

/**
 * The inspector that answers the desktop's cache view.
 *
 * Exists because the two platform inspectors it replaced answered the same question differently, and the
 * Android one answered it wrongly — it read a single `"${packageName}_preferences"` file while the
 * on-device console enumerated every store. An app writing to any other store name showed its keys on the
 * phone and an empty panel on the desktop. Nothing but a test keeps the two views on one definition.
 */
class RepositoryCacheInspectorTest {

    private class FakeCacheRepository(
        private val entries: List<CacheEntry>,
    ) : CacheRepository {
        var refreshCount = 0
            private set
        var getAllCount = 0
            private set

        override suspend fun getAllPreferences(): List<CacheEntry> {
            getAllCount++
            return entries
        }

        override fun observeAllPreferences(): Flow<List<CacheEntry>> = flowOf(entries)

        override suspend fun refresh(): List<CacheEntry> {
            refreshCount++
            return entries
        }

        override suspend fun getStores(): List<CacheStore> = emptyList()

        override fun getTotalSize(entries: List<CacheEntry>): String = "0 B"
    }

    private fun entry(key: String, value: String, store: String) = CacheEntry(
        key = key,
        value = value,
        type = CacheType.STRING,
        source = CacheSource.SHARED_PREFERENCES,
        storeName = store,
    )

    @Test
    fun `keys come from every store the repository discovered`() = runTest {
        // The regression: these two keys live in a store that is not "<package>_preferences", which the
        // old Android inspector was the only thing reading.
        val repository = FakeCacheRepository(
            listOf(
                entry("username", "yash", store = "android_sample_prefs"),
                entry("auto_refresh", "true", store = "android_sample_prefs"),
                entry("migrated", "1", store = "other_prefs"),
            ),
        )

        val keys = RepositoryCacheInspector(repository).getAllKeys()

        assertEquals(listOf("username", "auto_refresh", "migrated"), keys)
    }

    @Test
    fun `getAllKeys rescans rather than serving a previous session's list`() = runTest {
        val repository = FakeCacheRepository(listOf(entry("username", "yash", "prefs")))

        RepositoryCacheInspector(repository).getAllKeys()

        assertEquals(
            1,
            repository.refreshCount,
            "a connect must see the device's current preferences",
        )
    }

    @Test
    fun `getValue reads the primed cache rather than rescanning`() = runTest {
        // Load-bearing for cost: the desktop asks for one value per key, so a rescan here would be
        // quadratic in the number of preferences.
        val repository = FakeCacheRepository(
            listOf(entry("username", "yash", "prefs"), entry("theme", "dark", "prefs")),
        )
        val inspector = RepositoryCacheInspector(repository)

        inspector.getAllKeys()
        inspector.getValue("username")
        inspector.getValue("theme")

        assertEquals(1, repository.refreshCount, "rescanned once per requested value")
        assertEquals(2, repository.getAllCount)
    }

    @Test
    fun `a value is returned for a key in any store`() = runTest {
        val repository = FakeCacheRepository(
            listOf(entry("migrated", "1", store = "other_prefs")),
        )

        assertEquals("1", RepositoryCacheInspector(repository).getValue("migrated"))
    }

    @Test
    fun `an unknown key has no value`() = runTest {
        val repository = FakeCacheRepository(listOf(entry("username", "yash", "prefs")))

        assertNull(RepositoryCacheInspector(repository).getValue("nope"))
    }

    @Test
    fun `a key present in two stores is reported once`() = runTest {
        // Documented limitation rather than desired behaviour: the wire carries no store name, so
        // colliding keys collapse. Pinned so the collapse stays deliberate and cannot become a duplicate
        // row on the desktop.
        val repository = FakeCacheRepository(
            listOf(
                entry("version", "1", store = "a_prefs"),
                entry("version", "2", store = "b_prefs"),
            ),
        )
        val inspector = RepositoryCacheInspector(repository)

        assertEquals(listOf("version"), inspector.getAllKeys())
        assertEquals(
            "1",
            inspector.getValue("version"),
            "must resolve to the first entry consistently",
        )
    }

    @Test
    fun `an empty cache yields no keys`() = runTest {
        assertTrue(
            RepositoryCacheInspector(FakeCacheRepository(emptyList())).getAllKeys().isEmpty(),
        )
    }
}
