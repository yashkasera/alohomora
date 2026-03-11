package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.TraceEntry
import kotlinx.coroutines.flow.Flow

internal interface TraceRepository {

    fun getAll(
        query: String = "",
        method: String = "",
        page: Int,
        pageSize: Int,
    ): Flow<List<TraceEntry>>

    fun count(
        query: String = "",
        method: String = "",
    ): Flow<Long>

    suspend fun insert(call: TraceEntry)
    suspend fun clear()
    fun getById(id: String): Flow<TraceEntry?>
}
