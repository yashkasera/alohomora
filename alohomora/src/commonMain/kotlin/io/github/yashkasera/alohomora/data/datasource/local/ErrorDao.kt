package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.Error
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Error] entities.
 */
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

    /**
     * Lists errors with pagination and query filtering.
     */
    @Query("SELECT id, place, reason, time, isViewed FROM Error WHERE stackTrace LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String, page: Int, pageSize: Int): Flow<List<Error>>

    /**
     * Counts errors matching the query.
     */
    @Query("SELECT COUNT(*) FROM Error WHERE stackTrace LIKE '%' || :query || '%'")
    fun count(query: String): Flow<Long>

    /**
     * Marks an error as viewed.
     */
    @Query("Update Error SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)
}
