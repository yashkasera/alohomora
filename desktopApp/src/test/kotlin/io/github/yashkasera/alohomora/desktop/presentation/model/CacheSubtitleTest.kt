package io.github.yashkasera.alohomora.desktop.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals

/** The Cache subtitle, which is where the panel admits what it cannot search yet. */
class CacheSubtitleTest {

    private fun state(total: Int, shown: Int, loaded: Int) = CacheUiState(
        rows = (1..shown).map { CacheRow("k$it", "v$it", isLoaded = true) },
        totalCount = total,
        loadedCount = loaded,
    )

    @Test
    fun `a fully loaded unfiltered cache reports only its count`() {
        assertEquals("24 keys", cacheSubtitle(state(total = 24, shown = 24, loaded = 24)))
    }

    @Test
    fun `a single key is not pluralised`() {
        assertEquals("1 key", cacheSubtitle(state(total = 1, shown = 1, loaded = 1)))
    }

    @Test
    fun `a filtered cache reports the shown count`() {
        assertEquals("24 keys · 3 shown", cacheSubtitle(state(total = 24, shown = 3, loaded = 24)))
    }

    @Test
    fun `values still in flight are reported as loading`() {
        // The count the reader needs to interpret a miss: those values are not searchable yet.
        assertEquals("24 keys · 4 loading", cacheSubtitle(state(total = 24, shown = 24, loaded = 20)))
    }

    @Test
    fun `a filtered and still loading cache reports both`() {
        assertEquals(
            "24 keys · 3 shown · 4 loading",
            cacheSubtitle(state(total = 24, shown = 3, loaded = 20)),
        )
    }

    @Test
    fun `an empty cache still reports a count`() {
        assertEquals("0 keys", cacheSubtitle(CacheUiState()))
    }
}
