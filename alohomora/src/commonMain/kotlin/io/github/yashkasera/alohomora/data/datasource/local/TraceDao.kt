package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.yashkasera.alohomora.common.TraceEntry
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [TraceEntry] entities (network traces).
 */
@Dao
internal interface TraceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TraceEntry): Long

    @Query("SELECT * FROM TraceEntry WHERE id = :id")
    fun getById(id: String): Flow<TraceEntry?>

    @Query("DELETE FROM TraceEntry")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: TraceEntry)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: TraceEntry): Int

    /**
     * Lists traces with pagination, filtering by path query and HTTP method.
     */
    @Query("SELECT id, status, host, path, `query`, method, duration, time, isViewed FROM TraceEntry WHERE path LIKE '%' || :query || '%' AND method LIKE '%' || :method || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String?, method: String?, page: Int, pageSize: Int): Flow<List<TraceEntry>>

    /**
     * Gets latest traces with full data.
     */
    @Query("SELECT * FROM TraceEntry ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<TraceEntry>

    /**
     * Observes latest traces with full data for realtime streaming.
     */
    @Query("SELECT * FROM TraceEntry ORDER BY time DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<TraceEntry>>

    /**
     * Marks a trace as viewed.
     */
    @Query("Update TraceEntry SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: String)
}
