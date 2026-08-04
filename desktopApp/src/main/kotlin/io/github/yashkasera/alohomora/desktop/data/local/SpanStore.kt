package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.Span
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captured spans, **newest first**, matching [EventStore], [ErrorStore] and [TrafficStore].
 *
 * Ordered and deduplicated by `id` rather than by either timestamp, for a reason specific to spans: a
 * parent ends *after* its children, a later-started sibling can end before an earlier one, and two
 * spans can share a timestamp. Only the device's rowid is monotonic, which is why the device streams
 * `ORDER BY id DESC` and why the stream adapter keys on it.
 *
 * Holds spans flat and lets the ViewModel group them into traces. Grouping here would mean re-grouping
 * on every single streamed span — O(n²) across a session — for a value only the Traces panel reads.
 */
class SpanStore {
    private val _spans = MutableStateFlow<List<Span>>(emptyList())
    val spans: StateFlow<List<Span>> = _spans.asStateFlow()

    private val _captureSupported = MutableStateFlow(false)

    /**
     * Whether the connected app has span capture wired up at all.
     *
     * Defaults to false and is only ever set from the device's snapshot, mirroring how [ReplayStore]
     * reports replay support. Two things depend on it: an empty panel can say "this app has no tracer"
     * rather than "no traces yet", and `REQUEST_TRACE_SPANS` is never sent to an app that predates span
     * capture and would decode it as an unknown type without replying.
     */
    val captureSupported: StateFlow<Boolean> = _captureSupported.asStateFlow()

    fun setCaptureSupported(supported: Boolean) {
        _captureSupported.value = supported
    }

    /**
     * Inserts [span] at the top, replacing a span already held under the same `spanId`.
     *
     * Replace rather than ignore, unlike [ErrorStore.append]: an error is immutable once recorded, but
     * a tracer can legitimately re-export a span after a failed flush, and the device reseeds its
     * stream adapter on every snapshot.
     */
    fun append(span: Span) {
        val current = _spans.value
        val existing = current.indexOfFirst { it.spanId == span.spanId }
        val next = if (existing >= 0) {
            current.toMutableList().also { it[existing] = span }
        } else {
            listOf(span) + current
        }
        _spans.value = next.evictWholeTraces()
    }

    /**
     * Merges the full span list of one trace, from `SNAPSHOT_TRACE_SPANS`.
     *
     * Used to backfill a trace whose earliest spans fell outside the device's snapshot window, so the
     * incoming list is authoritative for that `traceId` and replaces whatever was held for it.
     */
    fun mergeTrace(traceId: String, spans: List<Span>) {
        val others = _spans.value.filterNot { it.traceId == traceId }
        _spans.value = (spans + others)
            .sortedByDescending { it.id }
            .evictWholeTraces()
    }

    /** Replaces everything, normalising to newest-first rather than trusting the wire order. */
    fun replace(spans: List<Span>) {
        _spans.value = spans
            .sortedByDescending { it.id }
            .evictWholeTraces()
    }

    /** Dims a whole trace once opened; viewing is a trace-level act, as it is on the device. */
    fun markTraceViewed(traceId: String) {
        val current = _spans.value
        if (current.none { it.traceId == traceId && !it.isViewed }) return
        _spans.value = current.map {
            if (it.traceId == traceId && !it.isViewed) it.copy(isViewed = true) else it
        }
    }

    fun clear() {
        _spans.value = emptyList()
    }

    /**
     * Trims to the cap by dropping **whole traces**, oldest first — never individual spans.
     *
     * This is the one thing this store does differently from its siblings, and it is not a nicety.
     * Trimming a flat list by span count would cut a trace mid-way and leave the survivors permanently
     * parentless: the waterfall would render a subtree of orphans that can never be reunited, because
     * the missing parent was evicted rather than merely late. A partial trace that looks like a live
     * one is worse than no trace.
     */
    private fun List<Span>.evictWholeTraces(): List<Span> {
        if (size <= MAX_SPANS) return this
        // Traces in newest-first order, since the list already is.
        val traceOrder = LinkedHashSet<String>()
        forEach { traceOrder.add(it.traceId) }
        val sizes = groupingBy { it.traceId }.eachCount()

        val keep = mutableSetOf<String>()
        var running = 0
        for (traceId in traceOrder) {
            val next = running + (sizes[traceId] ?: 0)
            // Always keep the newest trace, even on its own it exceeds the cap: dropping it would show
            // an empty panel immediately after a big trace arrived.
            if (next > MAX_SPANS && keep.isNotEmpty()) break
            keep += traceId
            running = next
        }
        return filter { it.traceId in keep }
    }

    private companion object {
        /**
         * Above the traffic cap in rows but comparable in *records*: a trace is 10-25 spans, so this is
         * roughly 150-400 traces. The device only ever sends `SPAN_SNAPSHOT_LIMIT` (1000) per snapshot,
         * so this holds several snapshots' worth of streamed spans on top of one.
         */
        const val MAX_SPANS = 4000
    }
}
