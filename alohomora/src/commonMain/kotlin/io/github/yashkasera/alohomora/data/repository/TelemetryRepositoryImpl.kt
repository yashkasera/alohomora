package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

internal class TelemetryRepositoryImpl(private val db: AlohomoraDb) : TelemetryRepository {
    override fun getEvents(query: String, page: Int, pageSize: Int): Flow<List<TelemetryEvent>> =
        db.telemetryDao().getAll(query, page, pageSize)

    override fun getEventsCount(query: String): Flow<Long> =
        db.telemetryDao().getCount(query)

    override suspend fun trackEvent(name: String, properties: JsonElement?) {
        db.telemetryDao().insert(
            TelemetryEvent(
                time = Clock.System.now().toEpochMilliseconds(),
                name = name,
                properties = properties,
            ),
        )
    }

    override suspend fun clear() = db.telemetryDao().clear()
}
