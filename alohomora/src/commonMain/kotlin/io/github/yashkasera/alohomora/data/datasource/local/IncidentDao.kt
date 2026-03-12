package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.Incident
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Incident] entities.
 */
@Dao
internal interface IncidentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Incident): Long

    @Query("SELECT * FROM Incident WHERE id = :id")
    fun getById(id: Long): Flow<Incident?>

    @Query("DELETE FROM Incident")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: Incident)

    /**
     * Lists incidents with pagination and query filtering.
     */
    @Query("SELECT id, place, reason, time, isViewed FROM Incident WHERE stackTrace LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String, page: Int, pageSize: Int): Flow<List<Incident>>

    /**
     * Counts incidents matching the query.
     */
    @Query("SELECT COUNT(*) FROM Incident WHERE stackTrace LIKE '%' || :query || '%'")
    fun count(query: String): Flow<Long>

    /**
     * Marks an incident as viewed.
     */
    @Query("Update Incident SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)
}
