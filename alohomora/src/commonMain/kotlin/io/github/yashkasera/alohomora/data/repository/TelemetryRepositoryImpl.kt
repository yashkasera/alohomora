package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.JsonElement

internal class TelemetryRepositoryImpl(private val db: AlohomoraDb) : TelemetryRepository {

    // Base Repository implementations
    override fun list(query: String, page: Int, pageSize: Int): Flow<List<TelemetryEvent>> =
        db.telemetryDao().list(query, page, pageSize)

    override fun getById(id: Long): Flow<TelemetryEvent?> = flowOf(null)

    override suspend fun save(item: TelemetryEvent): Long {
        db.telemetryDao().insert(item)
        return item.id
    }

    override suspend fun clearAll() = db.telemetryDao().clearAll()

    override suspend fun markAsViewed(id: Long) {
        /* no-op */
    }

    // Telemetry-specific implementations
    override fun count(query: String): Flow<Long> =
        db.telemetryDao().count(query)

    override suspend fun trackEvent(name: String, properties: JsonElement?) {
        db.telemetryDao().insert(
            TelemetryEvent(
                time = Clock.System.now().toEpochMilliseconds(),
                name = name,
                properties = properties,
            ),
        )
    }
}
