package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.event
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterTestData.properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The search index's memoisation.
 *
 * This is the performance requirement made observable. Without the cache, searching inside payloads
 * either re-stringifies the whole store on every keystroke or re-stringifies every *neighbour* on every
 * streamed event — the second being the worse of the two on a chatty app. `computations` exists purely
 * so that cannot silently regress.
 */
class EventSearchIndexTest {

    @Test
    fun `reindexing an unchanged list builds no new haystack`() {
        val index = EventSearchIndex()
        val events = listOf(event(id = 1), event(id = 2))

        index.reindex(events)
        assertEquals(2, index.computations)

        index.reindex(events)
        assertEquals(2, index.computations, "re-stringified events it had already seen")
    }

    @Test
    fun `a streamed event costs one haystack not the whole store`() {
        val index = EventSearchIndex()
        val existing = (1L..50L).map { event(id = it, time = it) }
        index.reindex(existing)
        val baseline = index.computations

        index.reindex(listOf(event(id = 51, time = 51)) + existing)

        assertEquals(baseline + 1, index.computations)
    }

    @Test
    fun `marking an event viewed reuses the cached haystack`() {
        // markViewed copies the event, so instance identity is useless as a key. The id-plus-time key
        // is what survives the copy.
        val index = EventSearchIndex()
        val original = event(id = 1, time = 500)
        index.reindex(listOf(original))

        index.reindex(listOf(original.copy(isViewed = true)))

        assertEquals(1, index.computations)
    }

    @Test
    fun `an event dropped from the list is dropped from the cache`() {
        // The cache is rebuilt rather than pruned, so an evicted event must not keep its text alive for
        // the life of the window.
        val index = EventSearchIndex()
        val evicted = event(id = 1, time = 100)
        index.reindex(listOf(evicted, event(id = 2, time = 200)))
        val baseline = index.computations

        index.reindex(listOf(event(id = 2, time = 200)))
        index.reindex(listOf(evicted, event(id = 2, time = 200)))

        assertEquals(baseline + 1, index.computations, "kept a dropped event's haystack alive")
    }

    @Test
    fun `two events sharing an id but not a time get separate haystacks`() {
        // A device switch restarts row ids from 1 with different payloads behind them, so keying on id
        // alone would serve the previous device's text for the new device's event.
        val index = EventSearchIndex()

        val first = index.reindex(
            listOf(event(id = 1, time = 100, properties = properties("device" to "pixel"))),
        )
        val second = index.reindex(
            listOf(event(id = 1, time = 200, properties = properties("device" to "galaxy"))),
        )

        assertTrue(first.single().haystack.contains("pixel"))
        assertTrue(second.single().haystack.contains("galaxy"))
        assertEquals(2, index.computations)
    }

    @Test
    fun `a haystack is lowercased and carries both name and properties`() {
        val haystack = event(
            name = "App.Exception",
            properties = properties("Message" to "Boom"),
        ).searchHaystack()

        assertEquals(haystack, haystack.lowercase())
        assertTrue(haystack.contains("app.exception"))
        assertTrue(haystack.contains("boom"))
    }

    @Test
    fun `the index preserves list order`() {
        // The panel renders this list directly, and it must stay newest-first as the store hands it over.
        val index = EventSearchIndex()
        val events = listOf(event(id = 3, time = 300), event(id = 2, time = 200), event(id = 1, time = 100))

        val result = index.reindex(events)

        assertEquals(listOf(3L, 2L, 1L), result.map { it.event.id })
    }
}
