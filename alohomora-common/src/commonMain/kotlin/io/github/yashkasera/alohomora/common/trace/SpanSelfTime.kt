package io.github.yashkasera.alohomora.common.trace

import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.durationNanos

/**
 * Time spent in [span] itself rather than in the work it delegated to [directChildren].
 *
 * The most actionable number in a waterfall and the one nothing else in the console exposes: a 400 ms
 * root whose children account for 390 ms is a slow dependency, whereas a 400 ms root with 10 ms of
 * children is a slow *parent*. The bars alone cannot tell those apart at a glance.
 *
 * Children are **merged before subtracting**, not summed. Concurrent children overlap, and summing
 * their durations double-counts the overlap — enough to drive self time to zero (and, before the
 * clamp, negative) for any span that fans out in parallel, which is exactly the shape worth
 * measuring.
 *
 * Pass only direct children. Passing descendants would subtract the same interval at every level.
 */
fun selfTimeNanos(span: Span, directChildren: List<Span>): Long {
    val total = span.durationNanos()
    if (total <= 0L) return 0L
    if (directChildren.isEmpty()) return total

    val intervals = directChildren
        .map { it.startEpochNanos to maxOf(it.endEpochNanos, it.startEpochNanos) }
        // Clip to the parent: a child that outlives its parent (skew, or a mis-set parent id) would
        // otherwise subtract time the parent never spent and push self time to the clamp.
        .map { (start, end) ->
            start.coerceIn(span.startEpochNanos, span.endEpochNanos) to
                end.coerceIn(span.startEpochNanos, span.endEpochNanos)
        }
        .filter { it.second > it.first }
        .sortedBy { it.first }

    if (intervals.isEmpty()) return total

    var covered = 0L
    var currentStart = intervals.first().first
    var currentEnd = intervals.first().second
    intervals.drop(1).forEach { (start, end) ->
        if (start > currentEnd) {
            covered += currentEnd - currentStart
            currentStart = start
            currentEnd = end
        } else if (end > currentEnd) {
            currentEnd = end
        }
    }
    covered += currentEnd - currentStart

    // Clamped: even after clipping, a child whose timestamps are simply wrong can exceed the parent.
    return (total - covered).coerceAtLeast(0L)
}

/**
 * Self time for [span], picking its direct children out of a whole trace.
 *
 * Distinct name rather than an overload: both would be `(Span, List<Span>)`, and a caller passing
 * the whole trace to the children-only variant would get a silently wrong answer, not a compile
 * error.
 */
fun selfTimeInTrace(span: Span, allSpans: List<Span>): Long =
    selfTimeNanos(span, allSpans.filter { it.parentSpanId == span.spanId })
