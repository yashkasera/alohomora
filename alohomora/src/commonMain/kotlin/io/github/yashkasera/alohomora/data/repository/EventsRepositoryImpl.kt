package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonElement

internal class EventsRepositoryImpl(private val db: AlohomoraDb) : EventsRepository {

    override fun list(query: String, page: Int, pageSize: Int): Flow<List<Event>> =
        db.eventDao().list(query, page, pageSize)

    override fun getById(id: Long): Flow<Event?> = flowOf(null)

    override suspend fun save(item: Event): Long =
        db.eventDao().insert(item)

    override suspend fun clearAll() = db.eventDao().clearAll()

    override suspend fun markAsViewed(id: Long) = db.eventDao().markAsViewed(id)

    override fun count(query: String): Flow<Long> =
        db.eventDao().count(query)

    override suspend fun trackEvent(name: String, properties: JsonElement?) {
        db.eventDao().insert(
            Event(
                time = Clock.System.now().toEpochMilliseconds(),
                name = name,
                properties = properties,
            ),
        )
    }
}
