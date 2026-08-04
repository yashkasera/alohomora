package io.github.yashkasera.alohomora.common.trace

import io.github.yashkasera.alohomora.common.Span

internal const val TRACE = "0af7651916cd43dd8448eb211c80319c"

/**
 * Builds a span with only the fields a given test cares about.
 *
 * Ids are short and readable ("a", "b") rather than real 16-hex values: nothing under test parses
 * them, and `parent = "a"` is far easier to check against than a wall of hex. Id *normalisation* is
 * covered separately, in `SpanNormalisationTest`.
 */
internal fun span(
    id: String,
    parent: String? = null,
    start: Long = 0L,
    end: Long = start + 1_000_000L,
    name: String = "span-$id",
    traceId: String = TRACE,
    status: String = Span.STATUS_UNSET,
    viewed: Boolean = false,
    scope: String? = null,
): Span = Span(
    traceId = traceId,
    spanId = id,
    parentSpanId = parent,
    name = name,
    startEpochNanos = start,
    endEpochNanos = end,
    statusCode = status,
    isViewed = viewed,
    scopeName = scope,
)
