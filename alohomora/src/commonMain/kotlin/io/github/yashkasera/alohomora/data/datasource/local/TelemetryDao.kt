package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.TelemetryEvent
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TelemetryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: TelemetryEvent)

    @Query("SELECT * FROM TelemetryEvent WHERE name LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun getAll(query: String, page: Int, pageSize: Int): Flow<List<TelemetryEvent>>

    @Query("SELECT * FROM TelemetryEvent ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<TelemetryEvent>

    @Query("SELECT COUNT(*) FROM TelemetryEvent WHERE name LIKE '%' || :query || '%'")
    fun getCount(query: String): Flow<Long>

    @Delete
    suspend fun delete(entity: TelemetryEvent)

    @Query("DELETE FROM TelemetryEvent")
    suspend fun clear()

}
