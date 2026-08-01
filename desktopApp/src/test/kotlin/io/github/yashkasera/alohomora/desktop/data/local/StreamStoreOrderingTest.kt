package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Ordering contract for the streaming stores: **newest first**.
 *
 * The bug these pin: the device sends its initial snapshot `ORDER BY time DESC`, but `append` added
 * to the end — so the snapshot read newest-first while every streamed item landed at the bottom.
 * The dashboard then applied its own `asReversed()`, so the same data was ordered two different
 * ways in one app.
 */
class StreamStoreOrderingTest {

    private fun trace(id: String, time: Long, status: Int? = null) =
        TraceEntry(id = id, time = time, status = status, path = "/p$id")

    private fun event(id: Long, time: Long, name: String = "e$id") =
        TelemetryEvent(id = id, time = time, name = name, properties = null)

    // region ApiLogStore

    @Test
    fun `newly streamed traces appear at the top`() {
        val store = ApiLogStore()
        store.append(trace("a", time = 1_000))
        store.append(trace("b", time = 2_000))
        store.append(trace("c", time = 3_000))

        assertEquals(listOf("c", "b", "a"), store.logs.value.map { it.id })
    }

    @Test
    fun `a streamed trace lands above an existing snapshot`() {
        val store = ApiLogStore()
        // Exactly what the device sends: ORDER BY time DESC.
        store.replace(listOf(trace("old2", 2_000), trace("old1", 1_000)))

        store.append(trace("new", 3_000))

        assertEquals(listOf("new", "old2", "old1"), store.logs.value.map { it.id })
    }

    @Test
    fun `replace normalises an out-of-order snapshot`() {
        val store = ApiLogStore()
        // Do not trust the wire order — normalise it.
        store.replace(listOf(trace("a", 1_000), trace("c", 3_000), trace("b", 2_000)))

        assertEquals(listOf("c", "b", "a"), store.logs.value.map { it.id })
    }

    @Test
    fun `a trace updated in flight is replaced, not duplicated`() {
        val store = ApiLogStore()
        // The device re-sends a trace when its contents change: captured pending, then completed.
        store.append(trace("req", time = 1_000, status = null))
        store.append(trace("req", time = 1_000, status = 200))

        assertEquals(1, store.logs.value.size, "the same trace must not appear twice")
        assertEquals(200, store.logs.value.single().status, "the update must win")
    }

    @Test
    fun `an updated trace keeps its position rather than jumping to the top`() {
        val store = ApiLogStore()
        store.append(trace("a", 1_000))
        store.append(trace("b", 2_000))
        store.append(trace("c", 3_000))

        // 'a' completing does not make it newer — time is the request start.
        store.append(trace("a", 1_000, status = 500))

        assertEquals(listOf("c", "b", "a"), store.logs.value.map { it.id })
        assertEquals(500, store.logs.value.last().status)
    }

    @Test
    fun `the cap drops the oldest, not the newest`() {
        val store = ApiLogStore()
        // 2001 entries; the very first one must be the one evicted.
        repeat(2_001) { index -> store.append(trace("t$index", time = index.toLong())) }

        val ids = store.logs.value.map { it.id }
        assertEquals(2_000, ids.size)
        assertEquals("t2000", ids.first(), "newest must be retained at the head")
        assertTrue("t0" !in ids, "the oldest entry must be the one evicted")
    }

    @Test
    fun `traces with no timestamp sort last instead of throwing`() {
        val store = ApiLogStore()
        store.replace(listOf(TraceEntry(id = "null-time"), trace("timed", 1_000)))

        assertEquals(listOf("timed", "null-time"), store.logs.value.map { it.id })
    }

    // endregion

    // region EventStore

    @Test
    fun `newly streamed events appear at the top`() {
        val store = EventStore()
        store.append(event(1, time = 1_000))
        store.append(event(2, time = 2_000))

        assertEquals(listOf(2L, 1L), store.events.value.map { it.id })
    }

    @Test
    fun `a streamed event lands above an existing snapshot`() {
        val store = EventStore()
        store.replace(listOf(event(2, 2_000), event(1, 1_000)))

        store.append(event(3, 3_000))

        assertEquals(listOf(3L, 2L, 1L), store.events.value.map { it.id })
    }

    @Test
    fun `redelivering the same event does not duplicate it`() {
        val store = EventStore()
        store.append(event(1, 1_000))
        store.append(event(1, 1_000))

        assertEquals(1, store.events.value.size)
    }

    @Test
    fun `event cap drops the oldest`() {
        val store = EventStore()
        repeat(2_001) { index -> store.append(event(index.toLong(), time = index.toLong())) }

        val ids = store.events.value.map { it.id }
        assertEquals(2_000, ids.size)
        assertEquals(2_000L, ids.first())
        assertTrue(0L !in ids)
    }

    // endregion
}
