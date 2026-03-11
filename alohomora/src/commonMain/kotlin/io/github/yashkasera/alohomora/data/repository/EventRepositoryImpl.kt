package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.common.Analytics
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

internal class EventRepositoryImpl(private val db: AlohomoraDb) : EventRepository {
    override fun getEvents(query: String, page: Int, pageSize: Int): Flow<List<Analytics>> =
        db.eventDao().getAll(query, page, pageSize)

    override fun getEventsCount(query: String): Flow<Long> =
        db.eventDao().getCount(query)

    override suspend fun trackEvent(name: String, properties: JsonElement?) {
        db.eventDao().insert(
            Analytics(
                time = Clock.System.now().toEpochMilliseconds(),
                name = name,
                properties = properties,
            ),
        )
    }

    override suspend fun clear() = db.eventDao().clear()
}
