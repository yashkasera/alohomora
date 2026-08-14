package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Event
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

internal interface EventsRepository : Repository<Event, Long> {

    fun count(query: String = ""): Flow<Long>

    suspend fun trackEvent(name: String, properties: JsonElement?)
}
