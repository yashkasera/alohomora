package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.data.entity.Analytics
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

internal class EventRepositoryImpl(private val db: AlohomoraDb) : EventRepository {
    override fun getAllEvents(): Flow<List<Analytics>> = db.eventDao().getAll("", 0, 20)

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
