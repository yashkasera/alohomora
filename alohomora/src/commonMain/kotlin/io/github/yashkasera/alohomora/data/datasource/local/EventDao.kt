package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.Event
import kotlinx.coroutines.flow.Flow

@Dao
internal interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Event): Long

    @Query("SELECT * FROM Event WHERE id = :id")
    fun getById(id: Long): Flow<Event?>

    @Query("DELETE FROM Event")
    suspend fun clearAll()

    @Query("UPDATE Event SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)

    @Delete
    suspend fun delete(entity: Event)

    /**
     * Lists events with pagination and query filtering.
     */
    @Query("SELECT * FROM Event WHERE name LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String, page: Int, pageSize: Int): Flow<List<Event>>

    /**
     * Counts events matching the query.
     */
    @Query("SELECT COUNT(*) FROM Event WHERE name LIKE '%' || :query || '%'")
    fun count(query: String): Flow<Long>

    /**
     * Gets latest events.
     */
    @Query("SELECT * FROM Event ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<Event>
}
