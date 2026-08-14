package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.Error
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ErrorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Error): Long

    @Query("SELECT * FROM Error WHERE id = :id")
    fun getById(id: Long): Flow<Error?>

    @Query("DELETE FROM Error")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: Error)

    @Query("SELECT id, place, reason, time, isViewed FROM Error WHERE reason LIKE '%' || :query || '%' OR place LIKE '%' || :query || '%' OR stackTrace LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String, page: Int, pageSize: Int): Flow<List<Error>>

    @Query("SELECT COUNT(*) FROM Error WHERE reason LIKE '%' || :query || '%' OR place LIKE '%' || :query || '%' OR stackTrace LIKE '%' || :query || '%'")
    fun count(query: String): Flow<Long>

    @Query("Update Error SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)

    /**
     * Unlike [list], includes `stackTrace` — the desktop snapshot needs it because the desktop
     * has no follow-up request for fetching one. Ordered by `id` (not `time`) for stable ordering
     * when two errors land in the same millisecond.
     */
    @Query("SELECT * FROM Error ORDER BY id DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<Error>

    @Query("SELECT * FROM Error ORDER BY id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<Error>>

    @Query("SELECT id, place, reason, time, isViewed FROM Error WHERE isViewed = 0 ORDER BY time DESC LIMIT :limit")
    fun observeUnviewed(limit: Int = 50): Flow<List<Error>>
}
