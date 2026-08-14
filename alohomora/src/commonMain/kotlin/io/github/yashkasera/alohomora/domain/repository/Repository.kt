package io.github.yashkasera.alohomora.domain.repository

import kotlinx.coroutines.flow.Flow

internal interface Repository<T, ID> {

    fun list(query: String = "", page: Int = 0, pageSize: Int = 20): Flow<List<T>>

    fun getById(id: ID): Flow<T?>

    suspend fun save(item: T): ID

    suspend fun clearAll()

    suspend fun markAsViewed(id: ID)
}
