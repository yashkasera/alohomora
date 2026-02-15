package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Crash
import kotlinx.coroutines.flow.Flow

internal interface CrashRepository {
    fun getAllCrashes(query: String, page: Int, pageSize: Int): Flow<List<Crash>>
    fun getCrashById(id: Long): Flow<Crash?>
    suspend fun insertCrash(crash: Crash): Long
    suspend fun deleteCrash(crash: Crash)
    suspend fun clearAll()
    suspend fun markAsViewed(id: Long)
}
