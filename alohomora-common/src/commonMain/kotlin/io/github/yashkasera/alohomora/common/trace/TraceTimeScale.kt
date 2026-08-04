package io.github.yashkasera.alohomora.common.trace

import io.github.yashkasera.alohomora.common.NANOS_PER_MILLI
import io.github.yashkasera.alohomora.common.NANOS_PER_SECOND
import io.github.yashkasera.alohomora.common.Span

/** Nanoseconds per microsecond. */
private const val NANOS_PER_MICRO = 1_000L

/**
 * The time range a waterfall maps onto its horizontal axis.
 *
 * Derived from the trace's own spans, which is what guarantees nothing renders off-canvas: every
 * span's start and end lie inside a window built from their own min and max.
 */
data class TraceWindow(
    val startNanos: Long,
    val endNanos: Long,
) {
    /**
     * Width of the window, never less than 1.
     *
     * A trace of one instantaneous span has `start == end`, and dividing by a zero range would make
     * every fraction NaN — which renders as nothing at all, so the one span in the trace vanishes.
     */
    val rangeNanos: Long get() = (endNanos - startNanos).coerceAtLeast(1L)

    /** Where [nanos] sits in the window, as `0f..1f`. Clamped, so a skewed timestamp cannot escape. */
    fun fractionOf(nanos: Long): Float =
        ((nanos - startNanos).toDouble() / rangeNanos.toDouble()).toFloat().coerceIn(0f, 1f)

    /** Offset of [nanos] from the window start, for an axis label. */
    fun offsetOf(nanos: Long): Long = nanos - startNanos
}

/**
 * The window enclosing [spans].
 *
 * `maxOf(end, start)` rather than plain `end` because a skewed span reports an end before its start,
 * and taking the smaller of the two would shrink the window to exclude a timestamp that the renderer
 * still has to place.
 */
fun traceWindow(spans: List<Span>): TraceWindow {
    if (spans.isEmpty()) return TraceWindow(0L, 1L)
    return TraceWindow(
        startNanos = spans.minOf { it.startEpochNanos },
        endNanos = spans.maxOf { maxOf(it.endEpochNanos, it.startEpochNanos) },
    )
}

/** Unit used for every label on one axis. */
enum class TimeUnitScale(val suffix: String, val nanosPerUnit: Long) {
    NANOSECONDS("ns", 1L),
    MICROSECONDS("µs", NANOS_PER_MICRO),
    MILLISECONDS("ms", NANOS_PER_MILLI),
    SECONDS("s", NANOS_PER_SECOND),
}

/**
 * Picks one unit for the whole axis, from its total range.
 *
 * Deliberately not per-label. Mixing units across one axis ("0, 500µs, 1ms, 1.5ms") defeats the
 * at-a-glance magnitude comparison the axis exists for.
 */
fun pickUnit(rangeNanos: Long): TimeUnitScale = when {
    rangeNanos < NANOS_PER_MICRO -> TimeUnitScale.NANOSECONDS
    rangeNanos < NANOS_PER_MILLI -> TimeUnitScale.MICROSECONDS
    rangeNanos < NANOS_PER_SECOND -> TimeUnitScale.MILLISECONDS
    else -> TimeUnitScale.SECONDS
}

/**
 * A round tick interval for [rangeNanos], from the 1/2/5×10^k ladder.
 *
 * The ladder is what keeps labels readable: a naive `range / targetTicks` yields intervals like
 * 137 ms, so the axis reads "0, 137, 274, 411" — arithmetically correct and useless for locating a
 * bar by eye. Snapping to 1, 2, 5, 10, 20, 50, … costs a tick or two of precision and buys labels
 * a reader can do mental arithmetic with.
 */
fun niceTickStep(rangeNanos: Long, targetTicks: Int = 6): Long {
    if (rangeNanos <= 0L || targetTicks <= 0) return 1L
    val rough = rangeNanos.toDouble() / targetTicks
    var magnitude = 1L
    // Long, not pow(): the range spans nanos to tens of seconds (~1e10), and a Double round-trip
    // through pow/log10 lands on 999_999_999 instead of 1_000_000_000 often enough to matter.
    while (magnitude * 10 <= rough && magnitude <= Long.MAX_VALUE / 10) {
        magnitude *= 10
    }
    return when {
        magnitude * 1 >= rough -> magnitude * 1
        magnitude * 2 >= rough -> magnitude * 2
        magnitude * 5 >= rough -> magnitude * 5
        else -> magnitude * 10
    }
}

/**
 * Tick offsets from the window start, at [step] intervals, inclusive of 0 and of the last tick that
 * fits inside the window.
 */
fun TraceWindow.ticks(step: Long = niceTickStep(rangeNanos)): List<Long> {
    if (step <= 0L) return listOf(0L)
    val result = mutableListOf<Long>()
    var tick = 0L
    while (tick <= rangeNanos) {
        result += tick
        // Guard against a step so small the loop would run for millions of iterations on a long
        // trace — a hostile step can only come from a caller passing one in.
        if (result.size >= MAX_TICKS) break
        tick += step
    }
    return result
}

private const val MAX_TICKS = 64

/**
 * Formats an offset for an axis label or a duration for a row, in [unit].
 *
 * Trailing zeros are dropped ("100ms", not "100.00ms") and the fraction is capped at two digits,
 * because an axis with "133.333333ms" on it is wider than the column it lives in.
 */
fun formatOffset(nanos: Long, unit: TimeUnitScale = pickUnit(nanos)): String {
    if (unit.nanosPerUnit == 1L) return "$nanos${unit.suffix}"
    val whole = nanos / unit.nanosPerUnit
    val fractionDigits = ((nanos % unit.nanosPerUnit) * 100 / unit.nanosPerUnit).toInt()
    if (fractionDigits == 0) return "$whole${unit.suffix}"
    val fraction = fractionDigits.toString().padStart(2, '0').trimEnd('0')
    return "$whole.$fraction${unit.suffix}"
}

/**
 * Formats a span's duration for a list row, picking the unit from the duration itself.
 *
 * A negative duration means clock skew; rendered as `0` with the caller expected to show a marker
 * rather than a nonsense negative reading.
 */
fun formatDuration(durationNanos: Long): String =
    if (durationNanos <= 0L) "0${pickUnit(0L).suffix}" else formatOffset(durationNanos)

/** Where a waterfall bar sits along its track, in the same pixel units the track was measured in. */
data class BarGeometry(val x: Float, val width: Float)

/**
 * Positions and sizes one bar along a track [trackWidth] wide.
 *
 * Pure, and deliberately not left inside the renderer's draw scope, because the clamping below is the
 * most consequential arithmetic in the whole waterfall and a draw scope is unreachable from a test.
 *
 * **The minimum width is the load-bearing rule.** A bar narrower than [minWidth] is widened to it,
 * which covers two cases that are the common case rather than corners:
 * - `start == end`, an instantaneous span, which would otherwise have zero width and vanish.
 * - Any span shorter than one pixel of the window. On a 1-second trace across 700px that is anything
 *   under roughly 1.4ms — which is most instrumented function calls.
 *
 * A span that is counted but not drawn is worse than useless: the waterfall then disagrees with the
 * span count in its own header.
 *
 * The x is also pulled back inside the track, so a min-width bar at the very end of a trace is not
 * clipped to nothing by overhanging the right edge.
 */
fun TraceWindow.barGeometry(
    startNanos: Long,
    endNanos: Long,
    trackWidth: Float,
    minWidth: Float,
): BarGeometry {
    // A skewed span reports an end before its start. Treated as instantaneous rather than corrected —
    // the caller flags it — because silently reordering impossible timestamps hides the bug.
    val safeEnd = maxOf(endNanos, startNanos)
    val startFraction = fractionOf(startNanos)
    val endFraction = fractionOf(safeEnd)
    val width = (trackWidth * (endFraction - startFraction)).coerceAtLeast(minWidth)
    val x = (trackWidth * startFraction).coerceAtMost((trackWidth - width).coerceAtLeast(0f))
    return BarGeometry(x = x, width = width)
}
