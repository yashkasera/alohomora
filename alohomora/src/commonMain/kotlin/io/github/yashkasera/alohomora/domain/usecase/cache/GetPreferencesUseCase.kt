package io.github.yashkasera.alohomora.domain.usecase.cache

import io.github.yashkasera.alohomora.domain.model.PreferenceEntry
import io.github.yashkasera.alohomora.domain.model.PreferenceStore
import io.github.yashkasera.alohomora.domain.repository.PreferenceRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case for getting all preferences from the repository.
 */
internal class GetPreferencesUseCase(
    private val repository: PreferenceRepository,
) {
    /**
     * Gets all preferences as a one-time operation.
     *
     * @return List of all preference entries
     */
    suspend operator fun invoke(): List<PreferenceEntry> = repository.getAllPreferences()

    /**
     * Observes all preferences as a flow for reactive updates.
     *
     * @return Flow of preference entries list
     */
    fun observe(): Flow<List<PreferenceEntry>> = repository.observeAllPreferences()

    /**
     * Refreshes the preferences list by re-scanning storage sources.
     *
     * @return Updated list of all preference entries
     */
    suspend fun refresh(): List<PreferenceEntry> = repository.refresh()

    /**
     * Gets information about all discovered preference stores.
     *
     * @return List of store metadata
     */
    suspend fun getStores(): List<PreferenceStore> = repository.getStores()

    /**
     * Calculates the total size of all preference entries.
     *
     * @param entries The entries to calculate size for
     * @return Human-readable size string
     */
    fun getTotalSize(entries: List<PreferenceEntry>): String = repository.getTotalSize(entries)
}
