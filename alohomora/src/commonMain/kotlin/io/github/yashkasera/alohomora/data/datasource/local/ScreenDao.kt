package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.data.entity.Screen
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ScreenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: Screen): Long

    @Query("SELECT * FROM Screen WHERE name LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun getAll(query: String, page: Int, pageSize: Int): Flow<List<Screen>>

    @Delete
    suspend fun delete(entity: Screen)

    @Query("DELETE FROM Screen")
    suspend fun deleteAll()

}
