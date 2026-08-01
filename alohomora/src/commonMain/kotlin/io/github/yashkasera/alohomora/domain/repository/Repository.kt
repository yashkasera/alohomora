package io.github.yashkasera.alohomora.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Base repository interface providing consistent CRUD patterns across all repositories.
 *
 * @param T The entity type
 * @param ID The identifier type (String for TrafficEntry, Long for Error/Event)
 */
internal interface Repository<T, ID> {

    /**
     * Retrieves a paginated list of entities matching the query.
     *
     * @param query Search string to filter results (empty for all)
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return Flow of entity list
     */
    fun list(query: String = "", page: Int = 0, pageSize: Int = 20): Flow<List<T>>

    /**
     * Retrieves a single entity by its ID.
     *
     * @param id Entity identifier
     * @return Flow of entity or null if not found
     */
    fun getById(id: ID): Flow<T?>

    /**
     * Inserts or updates an entity.
     *
     * @param item Entity to save
     * @return The entity ID (for auto-generated IDs) or the same ID (for assigned IDs)
     */
    suspend fun save(item: T): ID

    /**
     * Clears all entities from the repository.
     */
    suspend fun clearAll()

    /**
     * Marks an entity as viewed.
     *
     * @param id Entity identifier
     */
    suspend fun markAsViewed(id: ID)
}
