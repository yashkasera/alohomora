package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/**
 * Repository for telemetry events (analytics).
 * Extends base [Repository] with telemetry-specific tracking method.
 *
 * Note: [getById] is a no-op for telemetry — events are not fetched individually. [markAsViewed]
 * is implemented: events *are* opened one at a time, and the list dims the ones already read.
 */
internal interface TelemetryRepository : Repository<TelemetryEvent, Long> {

    /**
     * Gets the count of events matching the query.
     *
     * @param query Search string to filter by event name
     * @return Flow of count
     */
    fun count(query: String = ""): Flow<Long>

    /**
     * Records a new telemetry event with the given name and properties.
     *
     * @param name Event name/type
     * @param properties Optional JSON properties
     */
    suspend fun trackEvent(name: String, properties: JsonElement?)
}
