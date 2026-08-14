package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.Span
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ordering, dedup and eviction for [SpanStore].
 *
 * Extends the patterns in `StreamStoreOrderingTest` and `MarkViewedTest`, with one rule the other
 * stores do not have: eviction is per *trace*, never per span.
 */
class SpanStoreTest {

    private fun span(id: Long, traceId: String = "t1", spanId: String = "s$id") = Span(
        id = id,
        traceId = traceId,
        spanId = spanId,
        name = "span-$id",
        startEpochNanos = id * 1_000,
        endEpochNanos = id * 1_000 + 500,
    )

    @Test
    fun `replace normalises to newest-first rather than trusting wire order`() {
        val store = SpanStore()

        store.replace(listOf(span(1), span(3), span(2)))

        assertEquals(listOf(3L, 2L, 1L), store.spans.value.map { it.id })
    }

    @Test
    fun `append puts the newest span at the head`() {
        val store = SpanStore()
        store.replace(listOf(span(1), span(2)))

        store.append(span(3))

        assertEquals(listOf(3L, 2L, 1L), store.spans.value.map { it.id })
    }

    @Test
    fun `a redelivered span replaces rather than duplicating`() {
        // Unlike an error, a span can legitimately be re-exported: a tracer retries a failed flush, and
        // the device reseeds its stream adapter on every snapshot.
        val store = SpanStore()
        store.replace(listOf(span(1, spanId = "same")))

        store.append(span(1, spanId = "same").copy(name = "renamed"))

        assertEquals(1, store.spans.value.size)
        assertEquals("renamed", store.spans.value.single().name)
    }

    @Test
    fun `mergeTrace replaces only the named trace and leaves others alone`() {
        val store = SpanStore()
        store.replace(
            listOf(
                span(10, traceId = "keep", spanId = "k1"),
                span(9, traceId = "backfill", spanId = "b1"),
            ),
        )

        store.mergeTrace(
            "backfill",
            listOf(
                span(9, traceId = "backfill", spanId = "b1"),
                span(8, traceId = "backfill", spanId = "b2"),
            ),
        )

        assertEquals(1, store.spans.value.count { it.traceId == "keep" })
        assertEquals(2, store.spans.value.count { it.traceId == "backfill" })
        assertEquals(listOf(10L, 9L, 8L), store.spans.value.map { it.id })
    }

    @Test
    fun `marking a trace viewed flips every span in it and nothing else`() {
        val store = SpanStore()
        store.replace(
            listOf(
                span(3, traceId = "a", spanId = "a1"),
                span(2, traceId = "a", spanId = "a2"),
                span(1, traceId = "b", spanId = "b1"),
            ),
        )

        store.markTraceViewed("a")

        assertTrue(store.spans.value.filter { it.traceId == "a" }.all { it.isViewed })
        assertFalse(store.spans.value.single { it.traceId == "b" }.isViewed)
    }

    /**
     * The rule that distinguishes this store from its siblings.
     *
     * Trimming a flat list by span count would slice a trace mid-way and leave the survivors *permanently*
     * parentless — the waterfall would render orphans that can never be reunited, because the parent was
     * evicted rather than merely late. A partial trace that looks live is worse than no trace.
     */
    @Test
    fun `eviction drops whole traces, never partial ones`() {
        val store = SpanStore()
        // 5 traces of 1000 spans each = 5000, over the 4000 cap.
        val spans = (0 until 5).flatMap { t ->
            (0 until 1000).map { i ->
                val n = (t * 1000 + i).toLong()
                span(n, traceId = "trace$t", spanId = "t${t}s$i")
            }
        }.sortedByDescending { it.id }

        store.replace(spans)

        val held = store.spans.value.groupBy { it.traceId }
        assertTrue(held.isNotEmpty())
        held.forEach { (traceId, group) ->
            assertEquals(
                1000,
                group.size,
                "trace $traceId was sliced: ${group.size} of 1000 spans held",
            )
        }
        assertTrue(store.spans.value.size <= 4000)
    }

    @Test
    fun `eviction keeps the newest traces and drops the oldest`() {
        val store = SpanStore()
        val spans = (0 until 5).flatMap { t ->
            (0 until 1000).map { i ->
                val n = (t * 1000 + i).toLong()
                span(n, traceId = "trace$t", spanId = "t${t}s$i")
            }
        }.sortedByDescending { it.id }

        store.replace(spans)

        val heldTraces = store.spans.value.map { it.traceId }.toSet()
        assertTrue("trace4" in heldTraces, "the newest trace must survive")
        assertFalse("trace0" in heldTraces, "the oldest trace should have been evicted")
    }

    @Test
    fun `a single trace larger than the cap is kept rather than leaving an empty panel`() {
        // Truncating this to nothing would blank the panel at exactly the moment a big trace arrived.
        // Keeping it whole is the lesser evil; TraceWaterfall caps what it renders and says so.
        val store = SpanStore()
        val huge = (0 until 5000).map { span(it.toLong(), traceId = "one", spanId = "s$it") }
            .sortedByDescending { it.id }

        store.replace(huge)

        assertEquals(5000, store.spans.value.size)
    }

    @Test
    fun `capture support defaults to unsupported`() {
        // The desktop must not offer REQUEST_TRACE_SPANS to an app that predates span capture and will
        // never answer it.
        assertFalse(SpanStore().captureSupported.value)
    }

    @Test
    fun `clear empties the store`() {
        val store = SpanStore()
        store.replace(listOf(span(1), span(2)))

        store.clear()

        assertTrue(store.spans.value.isEmpty())
    }
}
