package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.Event
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement

/** Shared fixtures for the Events filter tests. */
internal object EventsFilterTestData {

    fun event(
        id: Long = 1,
        name: String = "App.Start",
        time: Long = 1_000,
        viewed: Boolean = false,
        properties: JsonElement? = null,
    ) = Event(id = id, name = name, properties = properties, time = time, isViewed = viewed)

    /** Mirrors what `Alohomora.recordEvent` produces from a property map. */
    fun properties(vararg pairs: Pair<String, String>): JsonElement =
        Json.encodeToJsonElement(pairs.toMap())

    /**
     * What `recordEvent` produces for a *null* property map: `Json.encodeToJsonElement(null)` yields
     * [JsonNull], not Kotlin null, which is the whole reason `prettyProperties` and `searchHaystack`
     * guard it.
     */
    fun nullProperties(): JsonElement = JsonNull

    /** Runs one event through the filter, indexing it the way the view model would. */
    fun matches(
        event: Event,
        filters: EventsFilterState = EventsFilterState(),
        floorMillis: Long? = null,
    ): Boolean = EventsPredicate(filters, floorMillis)
        .matches(IndexedEvent(event, event.searchHaystack()))
}
