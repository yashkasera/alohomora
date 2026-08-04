package io.github.yashkasera.alohomora.common.trace

import io.github.yashkasera.alohomora.common.Span
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TraceSummaryTest {

    private val other = "11111111111111111111111111111111"

    @Test
    fun `groups by traceId`() {
        val summaries = listOf(
            span("a", traceId = TRACE, start = 0),
            span("b", parent = "a", traceId = TRACE, start = 10),
            span("c", traceId = other, start = 20),
        ).toTraceSummaries()

        assertEquals(2, summaries.size)
        assertEquals(2, summaries.single { it.traceId == TRACE }.spanCount)
        assertEquals(1, summaries.single { it.traceId == other }.spanCount)
    }

    @Test
    fun `orders newest trace first`() {
        val summaries = listOf(
            span("old", traceId = TRACE, start = 1_000_000L),
            span("new", traceId = other, start = 5_000_000_000L),
        ).toTraceSummaries()

        assertEquals(listOf(other, TRACE), summaries.map { it.traceId })
    }

    @Test
    fun `traces starting in the same instant keep a stable order`() {
        // Without the traceId tie-break these rows swap places every time a late span widens one of
        // the windows — a list that reorders itself while it is being read.
        val spans = listOf(
            span("x", traceId = "bbbb", start = 0),
            span("y", traceId = "aaaa", start = 0),
        )

        assertEquals(
            spans.toTraceSummaries().map { it.traceId },
            spans.reversed().toTraceSummaries().map { it.traceId },
        )
    }

    @Test
    fun `duration is the wall-clock window rather than the sum of the spans`() {
        // Two concurrent 100ns children inside a 500ns root: summing would give 700.
        val summary = listOf(
            span("root", start = 0, end = 500),
            span("a", parent = "root", start = 100, end = 200),
            span("b", parent = "root", start = 100, end = 200),
        ).toTraceSummaries().single()

        assertEquals(500L, summary.durationNanos)
    }

    @Test
    fun `a child outliving its parent widens the trace duration`() {
        val summary = listOf(
            span("root", start = 0, end = 100),
            span("leaky", parent = "root", start = 50, end = 900),
        ).toTraceSummaries().single()

        assertEquals(900L, summary.durationNanos)
    }

    @Test
    fun `root name is null and the trace is incomplete until the root arrives`() {
        // The normal early state: the root encloses its children, so it ends — and therefore streams
        // — last. A trace is visible and nameless for as long as the operation is still running.
        val pending = listOf(span("child", parent = "root", start = 10)).toTraceSummaries().single()

        assertNull(pending.rootSpanName)
        assertFalse(pending.isComplete)

        val settled = listOf(
            span("child", parent = "root", start = 10),
            span("root", name = "GET /users", start = 0, end = 100),
        ).toTraceSummaries().single()

        assertEquals("GET /users", settled.rootSpanName)
        assertTrue(settled.isComplete)
    }

    @Test
    fun `hasError is true when any span failed`() {
        val summary = listOf(
            span("root", start = 0, end = 100),
            span("ok", parent = "root", start = 10),
            span("bad", parent = "root", start = 20, status = Span.STATUS_ERROR),
        ).toTraceSummaries().single()

        assertTrue(summary.hasError)
    }

    @Test
    fun `isViewed only when every span is viewed`() {
        val partly = listOf(
            span("root", viewed = true),
            span("child", parent = "root", viewed = false),
        ).toTraceSummaries().single()
        assertFalse(partly.isViewed)

        val fully = listOf(
            span("root", viewed = true),
            span("child", parent = "root", viewed = true),
        ).toTraceSummaries().single()
        assertTrue(fully.isViewed)
    }

    @Test
    fun `scope falls back to any span when the root has none`() {
        val summary = listOf(
            span("root", start = 0, end = 100, scope = null),
            span("child", parent = "root", start = 10, scope = "okhttp"),
        ).toTraceSummaries().single()

        assertEquals("okhttp", summary.scopeName)
    }

    @Test
    fun `summarize returns null for an empty list`() {
        assertNull(emptyList<Span>().summarize())
    }

    @Test
    fun `matches searches root name and scope and traceId prefix`() {
        val summary = listOf(
            span("root", name = "GET /users", start = 0, scope = "okhttp"),
        ).toTraceSummaries().single()

        assertTrue(summary.matches(""), "a blank query matches everything")
        assertTrue(summary.matches("users"))
        assertTrue(summary.matches("GET"))
        assertTrue(summary.matches("okhttp"))
        assertTrue(summary.matches(TRACE.take(8)))
        assertFalse(summary.matches("nothing here"))
    }

    @Test
    fun `matches is case insensitive`() {
        val summary = listOf(span("root", name = "GET /Users", start = 0)).toTraceSummaries().single()

        assertTrue(summary.matches("get /users"))
    }
}
