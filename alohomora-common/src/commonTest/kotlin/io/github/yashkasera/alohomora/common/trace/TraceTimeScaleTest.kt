package io.github.yashkasera.alohomora.common.trace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TraceTimeScaleTest {

    @Test
    fun `window spans min start to max end regardless of input order`() {
        val window = traceWindow(
            listOf(
                span("b", start = 500, end = 900),
                span("a", start = 100, end = 300),
                span("c", start = 200, end = 1_500),
            ),
        )

        assertEquals(100L, window.startNanos)
        assertEquals(1_500L, window.endNanos)
    }

    @Test
    fun `a skewed span cannot shrink the window below a timestamp still being rendered`() {
        val window = traceWindow(listOf(span("skewed", start = 900, end = 100)))

        assertEquals(900L, window.startNanos)
        assertEquals(900L, window.endNanos)
    }

    @Test
    fun `a zero-range window does not divide by zero`() {
        // A trace of one instantaneous span. NaN fractions render as nothing, so the trace's only
        // span would vanish.
        val window = traceWindow(listOf(span("only", start = 42, end = 42)))

        assertEquals(1L, window.rangeNanos)
        assertEquals(0f, window.fractionOf(42))
    }

    @Test
    fun `an empty span list yields a usable window`() {
        assertEquals(1L, traceWindow(emptyList()).rangeNanos)
    }

    @Test
    fun `fractionOf clamps outside the window`() {
        val window = TraceWindow(startNanos = 100, endNanos = 200)

        assertEquals(0f, window.fractionOf(50))
        assertEquals(0f, window.fractionOf(100))
        assertEquals(0.5f, window.fractionOf(150))
        assertEquals(1f, window.fractionOf(200))
        assertEquals(1f, window.fractionOf(9_999))
    }

    @Test
    fun `unit boundaries fall exactly on the powers of ten`() {
        assertEquals(TimeUnitScale.NANOSECONDS, pickUnit(999))
        assertEquals(TimeUnitScale.MICROSECONDS, pickUnit(1_000))
        assertEquals(TimeUnitScale.MICROSECONDS, pickUnit(999_999))
        assertEquals(TimeUnitScale.MILLISECONDS, pickUnit(1_000_000))
        assertEquals(TimeUnitScale.MILLISECONDS, pickUnit(999_999_999))
        assertEquals(TimeUnitScale.SECONDS, pickUnit(1_000_000_000))
    }

    @Test
    fun `tick step snaps to the 1-2-5 ladder`() {
        // A naive range/target yields intervals like 137ms, so an axis reads "0, 137, 274, 411" —
        // correct and useless for locating a bar by eye.
        assertEquals(200_000_000L, niceTickStep(rangeNanos = 1_000_000_000, targetTicks = 6))
        assertEquals(100L, niceTickStep(rangeNanos = 500, targetTicks = 6))
        assertEquals(1L, niceTickStep(rangeNanos = 5, targetTicks = 6))
    }

    @Test
    fun `tick step yields a readable number of ticks across ten orders of magnitude`() {
        // The property that matters is not the exact step but that no axis ends up with two ticks or
        // forty. Walks ns through tens of seconds.
        var range = 10L
        while (range <= 100_000_000_000L) {
            val ticks = TraceWindow(0, range).ticks()
            assertTrue(
                ticks.size in 3..13,
                "range $range produced ${ticks.size} ticks (step ${niceTickStep(range)})",
            )
            range *= 10
        }
    }

    @Test
    fun `tick step degrades safely on nonsense input`() {
        assertEquals(1L, niceTickStep(rangeNanos = 0))
        assertEquals(1L, niceTickStep(rangeNanos = -5))
        assertEquals(1L, niceTickStep(rangeNanos = 100, targetTicks = 0))
    }

    @Test
    fun `ticks start at zero and stay inside the window`() {
        val ticks = TraceWindow(startNanos = 1_000, endNanos = 1_500).ticks(step = 100)

        assertEquals(listOf(0L, 100L, 200L, 300L, 400L, 500L), ticks)
    }

    @Test
    fun `ticks are bounded even when handed a pathologically small step`() {
        // Only reachable from a caller passing a step in, but an unbounded loop here would freeze
        // the UI thread rather than fail visibly.
        val ticks = TraceWindow(0, 10_000_000_000L).ticks(step = 1)

        assertTrue(ticks.size <= 64, "got ${ticks.size} ticks")
    }

    @Test
    fun `offsets format with the unit and without trailing zeros`() {
        assertEquals("0ms", formatOffset(0, TimeUnitScale.MILLISECONDS))
        assertEquals("100ms", formatOffset(100_000_000, TimeUnitScale.MILLISECONDS))
        assertEquals("1.5ms", formatOffset(1_500_000, TimeUnitScale.MILLISECONDS))
        assertEquals("412ns", formatOffset(412, TimeUnitScale.NANOSECONDS))
        assertEquals("2.5s", formatOffset(2_500_000_000, TimeUnitScale.SECONDS))
    }

    @Test
    fun `a sub-millisecond duration keeps its precision instead of reading as zero`() {
        // The nanos decision under test: at millisecond storage this span would be "0ms".
        assertEquals("400µs", formatDuration(400_000))
        assertEquals("1.25µs", formatDuration(1_250))
    }

    @Test
    fun `a skewed duration reads as zero rather than negative`() {
        assertEquals("0ns", formatDuration(-5_000))
        assertEquals("0ns", formatDuration(0))
    }

    // ── bar geometry ────────────────────────────────────────────────────────────
    // The minimum-width clamp is the most consequential arithmetic in the waterfall, and it used to
    // live inside a DrawScope where no test could reach it. These are the assertions that made moving
    // it out worthwhile.

    private val track = 700f
    private val minWidth = 2f

    @Test
    fun `a bar spans the fraction of the track its span occupies`() {
        val window = TraceWindow(0, 1_000)

        val bar = window.barGeometry(250, 750, trackWidth = track, minWidth = minWidth)

        assertEquals(175f, bar.x)
        assertEquals(350f, bar.width)
    }

    @Test
    fun `an instantaneous span is widened to the minimum rather than vanishing`() {
        // start == end. Without the clamp this is a zero-width bar: the span is counted in the header
        // and invisible in the chart.
        val window = TraceWindow(0, 1_000_000_000)

        val bar = window.barGeometry(500_000_000, 500_000_000, track, minWidth)

        assertEquals(minWidth, bar.width)
    }

    @Test
    fun `a sub-pixel span is widened to the minimum`() {
        // A 1ms span in a 1s trace across 700px is 0.7px. Most instrumented calls are shorter than
        // this, so it is the common path, not a corner.
        val window = TraceWindow(0, 1_000_000_000)

        val bar = window.barGeometry(0, 1_000_000, track, minWidth)

        assertEquals(minWidth, bar.width)
    }

    @Test
    fun `a min-width bar at the very end of a trace stays inside the track`() {
        // Without pulling x back, this bar starts at exactly trackWidth and is clipped to nothing —
        // so the last span of every trace would disappear.
        val window = TraceWindow(0, 1_000)

        val bar = window.barGeometry(1_000, 1_000, track, minWidth)

        assertEquals(track - minWidth, bar.x)
        assertEquals(minWidth, bar.width)
        assertTrue(bar.x + bar.width <= track, "bar must not overhang the track")
    }

    @Test
    fun `a skewed bar renders as instantaneous at its start instead of inverted`() {
        val window = TraceWindow(0, 1_000)

        val bar = window.barGeometry(startNanos = 800, endNanos = 200, trackWidth = track, minWidth = minWidth)

        assertEquals(minWidth, bar.width, "a negative range must not produce a negative width")
        assertTrue(bar.x >= 0f)
    }

    @Test
    fun `a span covering the whole window fills the track`() {
        val window = TraceWindow(100, 200)

        val bar = window.barGeometry(100, 200, track, minWidth)

        assertEquals(0f, bar.x)
        assertEquals(track, bar.width)
    }

    @Test
    fun `a zero-range window still yields a drawable bar`() {
        val window = TraceWindow(42, 42)

        val bar = window.barGeometry(42, 42, track, minWidth)

        assertEquals(minWidth, bar.width)
    }

    @Test
    fun `a zero-width track degrades without producing a negative x`() {
        // Reachable for one frame before the first measure pass.
        val bar = TraceWindow(0, 100).barGeometry(0, 50, trackWidth = 0f, minWidth = minWidth)

        assertEquals(0f, bar.x)
        assertEquals(minWidth, bar.width)
    }
}
