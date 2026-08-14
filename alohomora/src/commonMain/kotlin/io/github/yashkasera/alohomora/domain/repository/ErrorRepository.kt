package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Error
import kotlinx.coroutines.flow.Flow

internal interface ErrorRepository : Repository<Error, Long> {

    suspend fun delete(error: Error)

    fun observeUnviewed(limit: Int = 50): Flow<List<Error>>
}
