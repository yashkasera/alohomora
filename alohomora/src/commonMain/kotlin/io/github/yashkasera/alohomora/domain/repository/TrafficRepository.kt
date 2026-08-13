package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlinx.coroutines.flow.Flow

/**
 * Repository for network traffic entries (API requests/responses).
 * Extends base [Repository] with Traffic-specific filtering by HTTP method.
 */
internal interface TrafficRepository : Repository<TrafficEntry, String> {

    /**
     * Retrieves traffic entries filtered by query and HTTP method.
     *
     * @param query Search string to filter by path
     * @param method HTTP method filter (GET, POST, etc.) - empty for all
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return Flow of traffic entries
     */
    fun list(
        query: String = "",
        method: String = "",
        page: Int = 0,
        pageSize: Int = 20,
    ): Flow<List<TrafficEntry>>

    /**
     * Observes the traffic entry produced by replaying [sourceId].
     *
     * A Flow, not a suspend read: capture persists the replay from its own coroutine, so it is
     * routinely absent at the moment the replay handler returns. A one-shot read would report
     * nothing and the console would look like the replay never happened.
     */
    fun observeReplayOf(sourceId: String): Flow<TrafficEntry?>

    fun observeUnviewedFailed(limit: Int = 50): Flow<List<TrafficEntry>>
}
