package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.datasource.local.TraceDao
import io.github.yashkasera.alohomora.domain.repository.TraceRepository
import kotlinx.coroutines.flow.Flow

internal class TraceRepositoryImpl(private val dao: TraceDao) : TraceRepository {

    override fun getAll(
        query: String,
        method: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<TraceEntry>> =
        dao.getAll(query = query, method = method, page = page, pageSize = pageSize)

    override fun count(
        query: String,
        method: String,
    ): Flow<Long> = dao.getCount(query, method)

    override suspend fun insert(call: TraceEntry) {
        dao.insert(call)
    }

    override suspend fun clear() = dao.clear()

    override fun getById(id: String): Flow<TraceEntry?> = dao.getById(id)
}
