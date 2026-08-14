package io.github.yashkasera.alohomora.domain.usecase.cache

import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheStore
import io.github.yashkasera.alohomora.domain.repository.CacheRepository
import kotlinx.coroutines.flow.Flow

internal class GetCacheUseCase(
    private val repository: CacheRepository,
) {
    suspend operator fun invoke(): List<CacheEntry> = repository.getAllPreferences()

    fun observe(): Flow<List<CacheEntry>> = repository.observeAllPreferences()

    suspend fun refresh(): List<CacheEntry> = repository.refresh()

    suspend fun getStores(): List<CacheStore> = repository.getStores()

    fun getTotalSize(entries: List<CacheEntry>): String = repository.getTotalSize(entries)
}
