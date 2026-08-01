package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.TelemetryEvent
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [TelemetryEvent] entities (analytics events).
 */
@Dao
internal interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TelemetryEvent): Long

    @Query("SELECT * FROM TelemetryEvent WHERE id = :id")
    fun getById(id: Long): Flow<TelemetryEvent?>

    @Query("DELETE FROM TelemetryEvent")
    suspend fun clearAll()

    @Query("UPDATE TelemetryEvent SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)

    @Delete
    suspend fun delete(entity: TelemetryEvent)

    /**
     * Lists events with pagination and query filtering.
     */
    @Query("SELECT * FROM TelemetryEvent WHERE name LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String, page: Int, pageSize: Int): Flow<List<TelemetryEvent>>

    /**
     * Counts events matching the query.
     */
    @Query("SELECT COUNT(*) FROM TelemetryEvent WHERE name LIKE '%' || :query || '%'")
    fun count(query: String): Flow<Long>

    /**
     * Gets latest events.
     */
    @Query("SELECT * FROM TelemetryEvent ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<TelemetryEvent>
}
