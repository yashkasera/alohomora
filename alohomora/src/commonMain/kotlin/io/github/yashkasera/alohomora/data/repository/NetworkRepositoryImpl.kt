package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.data.datasource.local.ApiRequestDao
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow

internal class NetworkRepositoryImpl(private val dao: ApiRequestDao) : NetworkRepository {

    override fun getAll(
        query: String,
        method: String,
        page: Int,
        pageSize: Int,
    ): Flow<List<ApiRequest>> =
        dao.getAll(query = query, method = method, page = page, pageSize = pageSize)

    override fun count(
        query: String,
        method: String,
    ): Flow<Long> = dao.getCount(query, method)

    override suspend fun insert(call: ApiRequest) {
        dao.insert(call)
    }

    override suspend fun clear() = dao.clear()

    override fun getById(id: String): Flow<ApiRequest?> = dao.getById(id)
}
