package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.datasource.local.TrafficDao
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import kotlinx.coroutines.flow.Flow

internal class TrafficRepositoryImpl(private val dao: TrafficDao) : TrafficRepository {

    // Base Repository implementation - delegates to list with empty method filter
    override fun list(query: String, page: Int, pageSize: Int): Flow<List<TrafficEntry>> =
        list(query = query, method = "", page = page, pageSize = pageSize)

    // Trace-specific list with method filtering
    override fun list(
        query: String,
        method: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<TrafficEntry>> =
        dao.list(query = query, method = method, page = page, pageSize = pageSize)

    override fun getById(id: String): Flow<TrafficEntry?> = dao.getById(id)

    override fun observeReplayOf(sourceId: String): Flow<TrafficEntry?> =
        dao.observeReplayOf(sourceId)

    override suspend fun save(item: TrafficEntry): String {
        dao.insert(item)
        return item.id
    }

    override suspend fun clearAll() = dao.clearAll()

    override suspend fun markAsViewed(id: String) = dao.markAsViewed(id)

    override fun observeUnviewedFailed(limit: Int): Flow<List<TrafficEntry>> =
        dao.observeUnviewedFailed(limit)

    override fun count(): Flow<Long> = dao.count()
}
