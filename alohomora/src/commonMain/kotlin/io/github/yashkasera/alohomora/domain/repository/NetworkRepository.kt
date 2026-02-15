package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.ApiRequest
import kotlinx.coroutines.flow.Flow

internal interface NetworkRepository {
    fun getAllCalls(): Flow<List<ApiRequest>>
    suspend fun addCall(call: ApiRequest)
    suspend fun clear()
    fun getById(id: String): Flow<ApiRequest?>
}
