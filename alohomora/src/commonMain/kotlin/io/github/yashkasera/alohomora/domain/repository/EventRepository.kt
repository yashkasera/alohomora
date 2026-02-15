package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Analytics
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

internal interface EventRepository {
    fun getAllEvents(): Flow<List<Analytics>>
    suspend fun trackEvent(name: String, properties: JsonElement?)
    suspend fun clear()
}
