package io.github.yashkasera.alohomora.common.trace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceTreeTest {

    @Test
    fun `nests children under their parent in start order`() {
        val rows = listOf(
            span("root", start = 0, end = 100),
            span("late", parent = "root", start = 50, end = 60),
            span("early", parent = "root", start = 10, end = 20),
        ).toTraceRows()

        assertEquals(listOf("root", "early", "late"), rows.map { it.span.spanId })
        assertEquals(listOf(0, 1, 1), rows.map { it.depth })
    }

    @Test
    fun `promotes a span whose parent is absent to a root and flags it`() {
        // The normal streaming state, not an edge case: a parent ends after its children, so a child
        // routinely arrives first. Dropping it would blank the trace while it is still running.
        val rows = listOf(span("child", parent = "not-here", start = 10)).toTraceRows()

        assertEquals(1, rows.size)
        assertEquals(0, rows.single().depth)
        assertTrue(rows.single().isOrphan)
    }

    @Test
    fun `re-parents an orphan once its parent arrives`() {
        val child = span("child", parent = "root", start = 10, end = 20)
        val orphaned = listOf(child).toTraceRows()
        assertTrue(orphaned.single().isOrphan, "precondition: child is an orphan on its own")

        val reunited = listOf(child, span("root", start = 0, end = 100)).toTraceRows()

        assertEquals(listOf("root", "child"), reunited.map { it.span.spanId })
        assertEquals(listOf(0, 1), reunited.map { it.depth })
        assertFalse(reunited.last().isOrphan)
    }

    @Test
    fun `terminates on a two-span cycle and still surfaces both spans`() {
        // recordSpan is public, so a hand-written adapter can produce this. A debugging tool that
        // hangs on malformed input is worse than one that renders it oddly.
        val rows = listOf(
            span("a", parent = "b"),
            span("b", parent = "a"),
        ).toTraceRows()

        assertEquals(setOf("a", "b"), rows.map { it.span.spanId }.toSet())
        assertTrue(rows.all { it.depth == 0 }, "a cycle has no root, so both surface at depth 0")
    }

    @Test
    fun `terminates on a cycle reachable from a real root`() {
        val rows = listOf(
            span("root", start = 0, end = 100),
            span("a", parent = "root", start = 10),
            span("b", parent = "a", start = 20),
            span("c", parent = "b", start = 30),
            span("loop", parent = "c", start = 40),
        ).toTraceRows()

        assertEquals(5, rows.size)
        assertEquals("root", rows.first().span.spanId)
    }

    @Test
    fun `sibling order is stable across shuffled input`() {
        // Without the spanId tie-break, spans sharing a start timestamp swap rows on every rebuild —
        // and the tree is rebuilt on every arriving span, so the waterfall would reshuffle as it fills.
        val spans = listOf(
            span("root", start = 0, end = 100),
            span("y", parent = "root", start = 10),
            span("x", parent = "root", start = 10),
            span("z", parent = "root", start = 10),
        )

        val first = spans.toTraceRows().map { it.span.spanId }
        val second = spans.reversed().toTraceRows().map { it.span.spanId }
        val third = spans.shuffled().toTraceRows().map { it.span.spanId }

        assertEquals(listOf("root", "x", "y", "z"), first)
        assertEquals(first, second)
        assertEquals(first, third)
    }

    @Test
    fun `collapsing hides exactly the descendants and reports their count`() {
        val spans = listOf(
            span("root", start = 0, end = 100),
            span("a", parent = "root", start = 10),
            span("a1", parent = "a", start = 12),
            span("a2", parent = "a", start = 14),
            span("b", parent = "root", start = 50),
        )

        assertEquals(5, spans.toTraceRows().size)

        val collapsed = spans.toTraceRows(collapsed = setOf("a"))

        assertEquals(listOf("root", "a", "b"), collapsed.map { it.span.spanId })
        val a = collapsed.single { it.span.spanId == "a" }
        assertTrue(a.isCollapsed)
        assertTrue(a.hasChildren)
        assertEquals(2, a.descendantCount)
    }

    @Test
    fun `descendantCount counts the whole subtree rather than just direct children`() {
        val rows = listOf(
            span("root"),
            span("a", parent = "root"),
            span("a1", parent = "a"),
            span("a1x", parent = "a1"),
        ).toTraceRows()

        assertEquals(3, rows.single { it.span.spanId == "root" }.descendantCount)
    }

    @Test
    fun `a collapsed row keeps its subtree time window so a summary bar can still be drawn`() {
        // Collapsing should hide structure, not time. Otherwise collapsing a slow parent makes the
        // slowness itself disappear from the waterfall.
        val rows = listOf(
            span("root", start = 0, end = 1_000),
            span("a", parent = "root", start = 100, end = 200),
            span("a1", parent = "a", start = 120, end = 900),
        ).toTraceRows(collapsed = setOf("a"))

        val a = rows.single { it.span.spanId == "a" }
        assertEquals(100L, a.subtreeStartNanos)
        assertEquals(900L, a.subtreeEndNanos, "the deep child's end must widen the collapsed window")
    }

    @Test
    fun `flags a span whose reported end precedes its start`() {
        val rows = listOf(span("skewed", start = 500, end = 100)).toTraceRows()

        assertTrue(rows.single().hasSkew)
    }

    @Test
    fun `an empty span list yields no rows`() {
        assertEquals(emptyList(), emptyList<io.github.yashkasera.alohomora.common.Span>().toTraceRows())
    }

    @Test
    fun `multiple roots are ordered by start`() {
        val rows = listOf(
            span("second", start = 100),
            span("first", start = 0),
        ).toTraceRows()

        assertEquals(listOf("first", "second"), rows.map { it.span.spanId })
    }
}
