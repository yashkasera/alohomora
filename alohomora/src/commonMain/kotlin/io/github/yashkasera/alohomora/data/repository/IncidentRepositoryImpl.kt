package io.github.yashkasera.alohomora.data.repository

import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.data.db.AlohomoraDb
import io.github.yashkasera.alohomora.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow

internal class IncidentRepositoryImpl(private val db: AlohomoraDb) : IncidentRepository {
    override fun getAll(query: String, page: Int, pageSize: Int): Flow<List<Incident>> =
        db.incidentDao().getAll(query, page, pageSize)

    override fun getById(id: Long): Flow<Incident?> =
        db.incidentDao().getById(id)

    override suspend fun insert(incident: Incident): Long =
        db.incidentDao().insert(incident)

    override suspend fun delete(incident: Incident) =
        db.incidentDao().delete(incident)

    override suspend fun clearAll() =
        db.incidentDao().clear()

    override suspend fun markAsViewed(id: Long) =
        db.incidentDao().markAsViewed(id)
}
