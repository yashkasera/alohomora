package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Incident
import kotlinx.coroutines.flow.Flow

internal interface IncidentRepository {
    fun getAll(query: String, page: Int, pageSize: Int): Flow<List<Incident>>
    fun getById(id: Long): Flow<Incident?>
    suspend fun insert(incident: Incident): Long
    suspend fun delete(incident: Incident)
    suspend fun clearAll()
    suspend fun markAsViewed(id: Long)
}
