package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Marking rows as read on the desktop.
 *
 * The tint itself already existed on both platforms; nothing ever set the flag, so it never
 * rendered. These pin the setter so it cannot quietly go dead again.
 */
class MarkViewedTest {

    private fun trace(id: String, viewed: Boolean = false) =
        TraceEntry(id = id, time = 1_000, isViewed = viewed)

    private fun event(id: Long, viewed: Boolean = false) =
        TelemetryEvent(id = id, name = "e$id", properties = null, time = 1_000, isViewed = viewed)

    @Test
    fun `opening a trace marks only that trace`() {
        val store = ApiLogStore()
        store.replace(listOf(trace("a"), trace("b")))

        store.markViewed("a")

        assertTrue(store.logs.value.first { it.id == "a" }.isViewed)
        assertFalse(store.logs.value.first { it.id == "b" }.isViewed, "marked a neighbour by mistake")
    }

    @Test
    fun `marking a trace does not reorder the list`() {
        val store = ApiLogStore()
        store.replace(listOf(trace("c"), trace("b"), trace("a")))

        store.markViewed("b")

        // Rewriting the list is how this is implemented, so order is worth pinning: a row
        // jumping position when you click it would be far worse than no tint at all.
        assertEquals(listOf("c", "b", "a"), store.logs.value.map { it.id })
    }

    @Test
    fun `marking an unknown trace is a no-op`() {
        val store = ApiLogStore()
        store.replace(listOf(trace("a")))

        store.markViewed("nope")

        assertEquals(1, store.logs.value.size)
    }

    @Test
    fun `marking an already viewed trace changes nothing`() {
        val store = ApiLogStore()
        store.replace(listOf(trace("a", viewed = true)))
        val before = store.logs.value

        store.markViewed("a")

        // Same instance: no pointless state emission, so no recomposition of the whole list.
        assertTrue(before === store.logs.value)
    }

    @Test
    fun `opening an event marks only that event`() {
        val store = EventStore()
        store.replace(listOf(event(1), event(2)))

        store.markViewed(1)

        assertTrue(store.events.value.first { it.id == 1L }.isViewed)
        assertFalse(store.events.value.first { it.id == 2L }.isViewed)
    }

    @Test
    fun `a device snapshot overwrites local viewed state`() {
        // Worth knowing rather than discovering: desktop marking is local, and the device's
        // snapshot is authoritative, so a refresh resets rows the device still considers unread.
        val store = EventStore()
        store.replace(listOf(event(1)))
        store.markViewed(1)
        assertTrue(store.events.value.single().isViewed)

        store.replace(listOf(event(1)))

        assertFalse(store.events.value.single().isViewed)
    }
}
