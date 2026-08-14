package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheStore
import kotlinx.coroutines.flow.Flow

internal interface CacheRepository {

    suspend fun getAllPreferences(): List<CacheEntry>

    fun observeAllPreferences(): Flow<List<CacheEntry>>

    suspend fun refresh(): List<CacheEntry>

    suspend fun getStores(): List<CacheStore>

    fun getTotalSize(entries: List<CacheEntry>): String
}
