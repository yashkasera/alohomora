package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.utils.Config
import kotlinx.coroutines.flow.Flow

internal interface NetworkRepository {

    fun getAll(
        query: String = "",
        method: String = "",
        page: Int,
        pageSize: Int,
    ): Flow<List<ApiRequest>>

    fun count(
        query: String = "",
        method: String = "",
    ): Flow<Long>

    suspend fun insert(call: ApiRequest)
    suspend fun clear()
    fun getById(id: String): Flow<ApiRequest?>
}
