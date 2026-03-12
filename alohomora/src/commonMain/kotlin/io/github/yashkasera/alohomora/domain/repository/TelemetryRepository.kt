package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.TelemetryEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/**
 * Repository for telemetry events (analytics).
 * Extends base [Repository] with telemetry-specific tracking method.
 *
 * Note: [getById] and [markAsViewed] are no-ops for telemetry as events
 * are not individually reviewed like crashes or traces.
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
