package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.yashkasera.alohomora.common.TraceEntry
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TraceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TraceEntry): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: TraceEntry): Int

    @Query("SELECT id, status, host, path, `query`, method, duration, time, isViewed  FROM TraceEntry WHERE path LIKE '%' || :query || '%' AND method LIKE '%' || :method || '%' ORDER BY time DESC  LIMIT :pageSize OFFSET :page * :pageSize")
    fun getAll(query: String?, method: String?, page: Int, pageSize: Int): Flow<List<TraceEntry>>

    @Query("SELECT COUNT(*)  FROM TraceEntry WHERE path LIKE '%' || :query || '%' AND method LIKE '%' || :method || '%'")
    fun getCount(query: String?, method: String?): Flow<Long>

    @Query("SELECT id, status, host, path, `query`, method, isViewed FROM TraceEntry ORDER BY time DESC LIMIT 5")
    fun getLatest(): Flow<List<TraceEntry>>

    @Query("SELECT * FROM TraceEntry ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<TraceEntry>

    @Query("SELECT * FROM TraceEntry WHERE id = :id")
    fun getById(id: String): Flow<TraceEntry?>

    @Delete
    suspend fun delete(entity: TraceEntry)

    @Query("DELETE FROM TraceEntry")
    suspend fun clear()


    @Query("Update TraceEntry SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)
}
