package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheStore
import kotlinx.coroutines.flow.Flow

/**
 * Repository for accessing application preferences from various storage sources.
 * Supports SharedPreferences, EncryptedSharedPreferences, DataStore (Android)
 * and NSUserDefaults (iOS).
 */
internal interface CacheRepository {

    /**
     * Gets all preferences from all discovered stores.
     * This is a one-time fetch - use [observeAllPreferences] for continuous updates.
     *
     * @return List of all preference entries
     */
    suspend fun getAllPreferences(): List<CacheEntry>

    /**
     * Observes all preferences as a flow for reactive updates.
     *
     * @return Flow of preference entries list
     */
    fun observeAllPreferences(): Flow<List<CacheEntry>>

    /**
     * Refreshes the preferences list by re-scanning storage sources.
     *
     * @return Updated list of all preference entries
     */
    suspend fun refresh(): List<CacheEntry>

    /**
     * Gets information about all discovered preference stores.
     *
     * @return List of store metadata
     */
    suspend fun getStores(): List<CacheStore>

    /**
     * Calculates the total size of all preference entries.
     *
     * @param entries The entries to calculate size for
     * @return Human-readable size string (e.g., "12KB", "1.5MB")
     */
    fun getTotalSize(entries: List<CacheEntry>): String
}
