package io.github.yashkasera.alohomora.common.trace

import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.isError
import io.github.yashkasera.alohomora.common.startEpochMillis
import kotlinx.serialization.Serializable

/**
 * A trace as a list row: everything derived from the spans sharing one `traceId`.
 *
 * Derived, never persisted. A `Trace` table would need [rootSpanName] filled in at insert time, and
 * the root span *ends last* — it encloses its children — so the name is unknown for the trace's
 * entire lifetime and every subsequent span would have to `UPDATE` the summary row. That is a
 * mutating row with two writers, for a value that is one `groupBy` away.
 *
 * Computed here, in shared code, rather than in SQL on the device: the desktop has no database and
 * has to group streamed spans in Kotlin regardless, so a device-side aggregate query would be a
 * second implementation of this definition. That is exactly how the two consoles came to disagree
 * on an error row's title before `exceptionTypeName()` moved into this module.
 */
@Serializable
data class TraceSummary(
    val traceId: String,
    /**
     * The root span's name, or null while the root has not arrived.
     *
     * Null is the normal early state, not an error: the root ends last, so a trace is visible and
     * nameless for as long as the operation is still running.
     */
    val rootSpanName: String?,
    /** Milliseconds, for display alongside every other timestamp in the console. */
    val startMillis: Long,
    /** Wall-clock span of the whole trace: max end minus min start, not the sum of its spans. */
    val durationNanos: Long,
    val spanCount: Int,
    val hasError: Boolean,
    /** False while no span in the trace is a root — see [rootSpanName]. */
    val isComplete: Boolean,
    /** True only when every span in the trace is viewed; viewing is a trace-level act. */
    val isViewed: Boolean,
    val scopeName: String?,
) {
    /** Matches the desktop and mobile search fields against everything a user might type. */
    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return rootSpanName?.lowercase()?.contains(q) == true ||
            scopeName?.lowercase()?.contains(q) == true ||
            traceId.startsWith(q)
    }
}

/**
 * Groups a flat span list into trace summaries, newest first.
 *
 * The tie-break on [TraceSummary.traceId] is not decoration. Two traces can start in the same
 * nanosecond, and without a stable secondary key their rows swap places every time a late span
 * widens one of the windows — a list that reorders itself while you read it.
 */
fun List<Span>.toTraceSummaries(): List<TraceSummary> =
    groupBy { it.traceId }
        .values
        .mapNotNull { it.summarize() }
        .sortedWith(compareByDescending<TraceSummary> { it.startMillis }.thenBy { it.traceId })

/**
 * Summarises the spans of a single trace. Returns null for an empty list.
 *
 * Callers must pass spans that share a `traceId`; grouping is [toTraceSummaries]' job.
 */
fun List<Span>.summarize(): TraceSummary? {
    if (isEmpty()) return null
    val start = minOf { it.startEpochNanos }
    // maxOf over end, not start + duration: a skewed span's end precedes its start, and taking the
    // larger of the two would stretch the window to include a timestamp that never happened.
    val end = maxOf { maxOf(it.endEpochNanos, it.startEpochNanos) }
    val root = firstOrNull { it.parentSpanId == null }
    return TraceSummary(
        traceId = first().traceId,
        rootSpanName = root?.name,
        startMillis = minByOrNull { it.startEpochNanos }?.startEpochMillis() ?: 0L,
        durationNanos = end - start,
        spanCount = size,
        hasError = any { it.isError() },
        isComplete = root != null,
        isViewed = all { it.isViewed },
        scopeName = root?.scopeName ?: firstNotNullOfOrNull { it.scopeName },
    )
}
