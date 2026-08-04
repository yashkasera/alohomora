package io.github.yashkasera.alohomora.common.trace

import kotlin.test.Test
import kotlin.test.assertEquals

class SpanSelfTimeTest {

    @Test
    fun `a leaf span's self time is its whole duration`() {
        assertEquals(100L, selfTimeNanos(span("leaf", start = 0, end = 100), emptyList()))
    }

    @Test
    fun `sequential children subtract cleanly`() {
        val parent = span("p", start = 0, end = 100)
        val children = listOf(
            span("a", parent = "p", start = 10, end = 30),
            span("b", parent = "p", start = 40, end = 60),
        )

        assertEquals(60L, selfTimeNanos(parent, children))
    }

    @Test
    fun `concurrent children are merged rather than summed`() {
        // The whole reason this is not `total - children.sumOf { duration }`. Two fully overlapping
        // 80ns children inside a 100ns parent: summing gives 160, so self time reads as 0 (clamped
        // from -60) for exactly the fan-out shape worth measuring.
        val parent = span("p", start = 0, end = 100)
        val children = listOf(
            span("a", parent = "p", start = 10, end = 90),
            span("b", parent = "p", start = 10, end = 90),
        )

        assertEquals(20L, selfTimeNanos(parent, children))
    }

    @Test
    fun `partially overlapping children count their union once`() {
        val parent = span("p", start = 0, end = 100)
        val children = listOf(
            span("a", parent = "p", start = 10, end = 50),
            span("b", parent = "p", start = 40, end = 70),
        )

        // Union is 10..70, so 60 covered, 40 self.
        assertEquals(40L, selfTimeNanos(parent, children))
    }

    @Test
    fun `a child outliving its parent is clipped rather than driving self time negative`() {
        val parent = span("p", start = 0, end = 100)
        val children = listOf(span("leaky", parent = "p", start = 50, end = 900))

        assertEquals(50L, selfTimeNanos(parent, children))
    }

    @Test
    fun `a child starting before its parent is clipped at the parent's start`() {
        val parent = span("p", start = 100, end = 200)
        val children = listOf(span("early", parent = "p", start = 0, end = 150))

        assertEquals(50L, selfTimeNanos(parent, children))
    }

    @Test
    fun `a skewed parent reports zero rather than a negative self time`() {
        assertEquals(0L, selfTimeNanos(span("skewed", start = 500, end = 100), emptyList()))
    }

    @Test
    fun `a zero-duration parent reports zero`() {
        assertEquals(0L, selfTimeNanos(span("instant", start = 42, end = 42), emptyList()))
    }

    @Test
    fun `a zero-duration child covers nothing`() {
        val parent = span("p", start = 0, end = 100)

        assertEquals(100L, selfTimeNanos(parent, listOf(span("tick", parent = "p", start = 50, end = 50))))
    }

    @Test
    fun `selfTimeInTrace picks out direct children and ignores deeper descendants`() {
        // Passing descendants to the children-only variant would subtract the same interval at every
        // level, so the distinction is not cosmetic.
        val trace = listOf(
            span("root", start = 0, end = 100),
            span("a", parent = "root", start = 10, end = 90),
            span("a1", parent = "a", start = 20, end = 80),
        )

        assertEquals(20L, selfTimeInTrace(trace.first(), trace))
        assertEquals(20L, selfTimeInTrace(trace[1], trace))
        assertEquals(60L, selfTimeInTrace(trace[2], trace))
    }
}
