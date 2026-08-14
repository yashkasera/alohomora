package io.github.yashkasera.alohomora.common.trace

import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.hasSkew

/** A span and its children, as assembled by [buildTraceTree]. */
data class TraceNode(
    val span: Span,
    val children: List<TraceNode>,
)

/** One rendered line of a waterfall: a span plus everything the renderer needs that isn't on the span. */
data class TraceRow(
    val span: Span,
    val depth: Int,
    val hasChildren: Boolean,
    val isCollapsed: Boolean,
    /** Total descendants, for the `+N` badge on a collapsed row. */
    val descendantCount: Int,
    /** True when this span's parent is not in the trace — see [buildTraceTree]. */
    val isOrphan: Boolean,
    /** True when the span's reported end precedes its start. */
    val hasSkew: Boolean,
    /** Earliest start across this span and its descendants, for a collapsed row's summary bar. */
    val subtreeStartNanos: Long,
    /** Latest end across this span and its descendants. */
    val subtreeEndNanos: Long,
)

/**
 * Assembles spans into a forest.
 *
 * Three rules, all of which exist because spans arrive one at a time and out of order:
 *
 * **Orphans are promoted to roots, never hidden.** A span whose `parentSpanId` names a span not in
 * this list is treated as a root and flagged. This is the *normal* case, not an edge case: a parent
 * ends after its children, so while an operation is in flight every one of its children is
 * dangling. Dropping them would mean a trace renders as empty for as long as it is still running —
 * precisely when someone is watching it.
 *
 * **The forest is rebuilt from scratch on every call, never mutated in place.** That is what makes
 * re-parenting free: when the missing parent finally streams in, the next build puts its children
 * underneath it with no reconciliation code, no stale-pointer bookkeeping, and no way for the two to
 * disagree. Do not be tempted to make this incremental.
 *
 * **Cycles terminate.** `recordSpan` is public, so a hand-written adapter can produce a span that is
 * its own ancestor. Anything unreachable from a root after the walk is appended as a root rather
 * than recursed into, so a malformed trace renders oddly instead of hanging a debugging tool.
 *
 * Siblings are ordered by start, tie-broken on `spanId`. Without the tie-break, spans that start in
 * the same nanosecond swap rows on every rebuild — which, given the rebuild-per-arrival rule above,
 * means the waterfall reshuffles as it fills.
 */
fun buildTraceTree(spans: List<Span>): List<TraceNode> {
    if (spans.isEmpty()) return emptyList()

    // Last write wins on a duplicate spanId. The DB's unique index prevents it there, but this also
    // runs on the desktop over a streamed list that can legitimately redeliver a span.
    val bySpanId = spans.associateBy { it.spanId }
    val childrenByParent = spans
        .filter { it.parentSpanId != null && bySpanId.containsKey(it.parentSpanId) }
        .groupBy { it.parentSpanId!! }

    val rootSpans =
        spans.filter { it.parentSpanId == null || !bySpanId.containsKey(it.parentSpanId) }

    val visited = mutableSetOf<String>()
    val roots = rootSpans
        .sortedWith(spanOrder)
        .map { buildNode(it, childrenByParent, visited) }

    // Anything a root could not reach is in a cycle. Surface those spans as roots so they are still
    // inspectable, rather than silently vanishing from a trace whose span count says they exist.
    val unreachable = spans.filter { it.spanId !in visited }
    if (unreachable.isEmpty()) return roots
    return roots + unreachable.sortedWith(spanOrder).map { TraceNode(it, emptyList()) }
}

private fun buildNode(
    span: Span,
    childrenByParent: Map<String, List<Span>>,
    visited: MutableSet<String>,
): TraceNode {
    // Marking before recursing is what breaks a cycle: a descendant that points back here finds
    // itself already visited and stops.
    if (!visited.add(span.spanId)) return TraceNode(span, emptyList())
    val children = childrenByParent[span.spanId]
        ?.filter { it.spanId !in visited }
        ?.sortedWith(spanOrder)
        ?.map { buildNode(it, childrenByParent, visited) }
        ?: emptyList()
    return TraceNode(span, children)
}

private val spanOrder = compareBy<Span>({ it.startEpochNanos }, { it.spanId })

/**
 * Builds the forest and flattens it into renderable rows in display order, skipping the descendants
 * of anything in [collapsed].
 *
 * One entry point rather than a separate build-then-flatten pair, because orphan-ness is a property
 * of the *input list*, not of a [TraceNode] — the same span is an orphan in one snapshot and a child
 * in the next — so a flatten that did not also see the original list could only ever report
 * `isOrphan = false`.
 *
 * A collapsed row keeps its subtree's time window ([TraceRow.subtreeStartNanos] /
 * [TraceRow.subtreeEndNanos]) so the renderer can still draw a summary bar. Collapsing should hide
 * structure, not time — otherwise collapsing a slow parent makes the slowness disappear.
 */
fun List<Span>.toTraceRows(collapsed: Set<String> = emptySet()): List<TraceRow> {
    val presentIds = mapTo(mutableSetOf()) { it.spanId }
    val orphanIds = filter { it.parentSpanId != null && it.parentSpanId !in presentIds }
        .mapTo(mutableSetOf()) { it.spanId }
    val rows = mutableListOf<TraceRow>()
    buildTraceTree(this).forEach {
        appendRows(
            it,
            depth = 0,
            collapsed = collapsed,
            orphanIds = orphanIds,
            out = rows,
        )
    }
    return rows
}

private fun appendRows(
    node: TraceNode,
    depth: Int,
    collapsed: Set<String>,
    orphanIds: Set<String>,
    out: MutableList<TraceRow>,
) {
    val isCollapsed = node.span.spanId in collapsed
    val window = node.subtreeWindow()
    out += TraceRow(
        span = node.span,
        depth = depth,
        hasChildren = node.children.isNotEmpty(),
        isCollapsed = isCollapsed,
        descendantCount = node.descendantCount(),
        isOrphan = node.span.spanId in orphanIds,
        hasSkew = node.span.hasSkew(),
        subtreeStartNanos = window.first,
        subtreeEndNanos = window.second,
    )
    if (isCollapsed) return
    node.children.forEach { appendRows(it, depth + 1, collapsed, orphanIds, out) }
}

private fun TraceNode.descendantCount(): Int = children.sumOf { 1 + it.descendantCount() }

private fun TraceNode.subtreeWindow(): Pair<Long, Long> {
    var start = span.startEpochNanos
    var end = maxOf(span.endEpochNanos, span.startEpochNanos)
    children.forEach { child ->
        val (childStart, childEnd) = child.subtreeWindow()
        if (childStart < start) start = childStart
        if (childEnd > end) end = childEnd
    }
    return start to end
}
