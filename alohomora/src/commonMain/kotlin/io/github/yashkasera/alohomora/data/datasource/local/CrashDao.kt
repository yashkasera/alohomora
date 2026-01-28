package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.data.entity.Crash
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CrashDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Crash): Long

    @Query("SELECT id, place, reason, time, isViewed FROM Crash WHERE stackTrace LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun getAll(query: String, page: Int, pageSize: Int): Flow<List<Crash>>

    @Query("SELECT * FROM Crash WHERE id = :id")
    fun getById(id: Long): Flow<Crash?>

    @Delete
    suspend fun delete(entity: Crash)

    @Query("DELETE FROM Crash")
    suspend fun clear()

    @Query("Update Crash SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)
}
