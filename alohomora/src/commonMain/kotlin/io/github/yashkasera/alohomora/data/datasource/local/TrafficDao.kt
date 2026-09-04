package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TrafficDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TrafficEntry): Long

    @Query("SELECT * FROM TrafficEntry WHERE id = :id")
    fun getById(id: String): Flow<TrafficEntry?>

    @Query("DELETE FROM TrafficEntry")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: TrafficEntry)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: TrafficEntry): Int

    @Query("SELECT id, status, host, path, `query`, method, duration, time, isViewed, replayOf, requestBodyTruncated, responseBodyTruncated, mockedBy FROM TrafficEntry WHERE path LIKE '%' || :query || '%' AND method LIKE '%' || :method || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String?, method: String?, page: Int, pageSize: Int): Flow<List<TrafficEntry>>

    /**
     * A Flow rather than a one-shot read: capture writes the trace from its own coroutine, so it
     * is routinely not in the database at the moment the replay handler returns.
     */
    @Query("SELECT * FROM TrafficEntry WHERE replayOf = :sourceId ORDER BY time DESC LIMIT 1")
    fun observeReplayOf(sourceId: String): Flow<TrafficEntry?>

    @Query("SELECT * FROM TrafficEntry ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<TrafficEntry>

    @Query("SELECT * FROM TrafficEntry ORDER BY time DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<TrafficEntry>>

    @Query("SELECT COUNT(*) FROM TrafficEntry")
    fun count(): Flow<Long>

    @Query("Update TrafficEntry SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: String)

    @Query(
        "SELECT id, status, url, method, host, path, `query`, duration, time, isViewed," +
            " replayOf, requestBodyTruncated, responseBodyTruncated, mockedBy" +
            " FROM TrafficEntry" +
            " WHERE isViewed = 0 AND (status IS NULL OR status NOT BETWEEN 200 AND 299)" +
            " ORDER BY time DESC LIMIT :limit",
    )
    fun observeUnviewedFailed(limit: Int = 50): Flow<List<TrafficEntry>>
}
