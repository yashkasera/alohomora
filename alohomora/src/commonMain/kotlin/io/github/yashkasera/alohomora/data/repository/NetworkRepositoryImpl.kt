package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.datasource.local.ApiRequestDao
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.entity.ApiRequest
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow

internal class NetworkRepositoryImpl(private val dao: ApiRequestDao) : NetworkRepository {

    override fun getAllCalls(): Flow<List<ApiRequest>> =
        dao.getAll("", "", 0, 20)

    override suspend fun addCall(call: ApiRequest) {
        dao.insert(call)
    }

    override suspend fun clear() = dao.clear()

    override fun getById(id: String): Flow<ApiRequest?> = dao.getById(id)
}
