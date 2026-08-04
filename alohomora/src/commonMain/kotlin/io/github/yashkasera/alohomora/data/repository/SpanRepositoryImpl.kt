package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.SpanRepository
import kotlinx.coroutines.flow.Flow

internal class SpanRepositoryImpl(private val db: AlohomoraDb) : SpanRepository {

    override fun observeLatestSpans(limit: Int): Flow<List<Span>> =
        db.spanDao().observeLatest(limit)

    override fun observeTrace(traceId: String): Flow<List<Span>> =
        db.spanDao().observeTrace(traceId)

    override suspend fun save(span: Span) {
        db.spanDao().insert(span)
    }

    override suspend fun saveAll(spans: List<Span>) {
        if (spans.isEmpty()) return
        db.spanDao().insertAll(spans)
    }

    override suspend fun markTraceAsViewed(traceId: String) =
        db.spanDao().markTraceAsViewed(traceId)

    override suspend fun clearAll() =
        db.spanDao().clearAll()
}
