package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.datasource.local.TraceDao
import io.github.yashkasera.alohomora.domain.repository.TraceRepository
import kotlinx.coroutines.flow.Flow

internal class TraceRepositoryImpl(private val dao: TraceDao) : TraceRepository {

    // Base Repository implementation - delegates to list with empty method filter
    override fun list(query: String, page: Int, pageSize: Int): Flow<List<TraceEntry>> =
        list(query = query, method = "", page = page, pageSize = pageSize)

    // Trace-specific list with method filtering
    override fun list(
        query: String,
        method: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<TraceEntry>> =
        dao.list(query = query, method = method, page = page, pageSize = pageSize)

    override fun getById(id: String): Flow<TraceEntry?> = dao.getById(id)

    override fun observeReplayOf(sourceId: String): Flow<TraceEntry?> = dao.observeReplayOf(sourceId)

    override suspend fun save(item: TraceEntry): String {
        dao.insert(item)
        return item.id
    }

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun markAsViewed(id: String) = dao.markAsViewed(id)
}
