package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import kotlinx.coroutines.flow.Flow

internal class ErrorRepositoryImpl(private val db: AlohomoraDb) : ErrorRepository {

    override fun list(query: String, page: Int, pageSize: Int): Flow<List<Error>> =
        db.errorDao().list(query, page, pageSize)

    override fun getById(id: Long): Flow<Error?> =
        db.errorDao().getById(id)

    override suspend fun save(item: Error): Long {
        db.errorDao().insert(item)
        return item.id
    }

    override suspend fun delete(error: Error) =
        db.errorDao().delete(error)

    override suspend fun clearAll() =
        db.errorDao().clearAll()

    override suspend fun markAsViewed(id: Long) =
        db.errorDao().markAsViewed(id)
}
