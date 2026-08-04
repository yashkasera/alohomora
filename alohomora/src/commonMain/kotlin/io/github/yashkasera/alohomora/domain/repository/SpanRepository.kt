package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Span
import kotlinx.coroutines.flow.Flow

/**
 * Repository for captured [Span]s.
 *
 * Deliberately does **not** extend [Repository], and that is not laziness. The base interface
 * presumes one entity is both the write unit and the list unit. Spans break that: the write unit is a
 * single [Span], but the list unit is a *trace* — every span sharing a `traceId`. Forcing the fit
 * would mean a `getById(spanId)` nothing calls and a `markAsViewed(spanId)` that lies, because
 * viewing is a trace-level act. `CacheRepository` sets the precedent for a purpose-fit interface.
 *
 * There is also no paginated `list`. Traces are assembled from a window of recent spans in shared
 * Kotlin rather than paged out of SQL — see [SpanDao] for why.
 */
internal interface SpanRepository {

    /**
     * The most recent [limit] spans, newest first by rowid.
     *
     * Both consoles group these into traces with `toTraceSummaries()`. The window is bounded rather
     * than paged because a trace straddles rows: paging by span would slice a trace across page
     * boundaries and render half a waterfall.
     */
    fun observeLatestSpans(limit: Int): Flow<List<Span>>

    /** Every span of one trace, in start order. */
    fun observeTrace(traceId: String): Flow<List<Span>>

    suspend fun save(span: Span)

    /** One transaction for a whole batch, which is what a tracer's exporter hands over. */
    suspend fun saveAll(spans: List<Span>)

    suspend fun markTraceAsViewed(traceId: String)

    suspend fun clearAll()
}
