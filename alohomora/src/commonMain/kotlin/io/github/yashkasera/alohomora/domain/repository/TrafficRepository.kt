package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlinx.coroutines.flow.Flow

internal interface TrafficRepository : Repository<TrafficEntry, String> {

    fun list(
        query: String = "",
        method: String = "",
        page: Int = 0,
        pageSize: Int = 20,
    ): Flow<List<TrafficEntry>>

    /**
     * A Flow, not a suspend read: capture persists the replay from its own coroutine, so it is
     * routinely absent at the moment the replay handler returns. A one-shot read would report
     * nothing and the console would look like the replay never happened.
     */
    fun observeReplayOf(sourceId: String): Flow<TrafficEntry?>

    fun observeUnviewedFailed(limit: Int = 50): Flow<List<TrafficEntry>>

    fun count(): Flow<Long>
}
