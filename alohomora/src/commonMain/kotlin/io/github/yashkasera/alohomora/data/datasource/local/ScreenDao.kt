package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.Screen
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Screen] entities (screen view tracking).
 */
@Dao
internal interface ScreenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Screen): Long

    @Query("SELECT * FROM Screen WHERE id = :id")
    fun getById(id: Long): Flow<Screen?>

    @Query("DELETE FROM Screen")
    suspend fun clearAll()

    @Delete
    suspend fun delete(entity: Screen)

    /**
     * Lists screens with pagination and query filtering.
     */
    @Query("SELECT * FROM Screen WHERE name LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun list(query: String, page: Int, pageSize: Int): Flow<List<Screen>>
}
