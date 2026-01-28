package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.data.entity.Analytics
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AnalyticsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: Analytics)

    @Query("SELECT * FROM Analytics WHERE name LIKE '%' || :query || '%' ORDER BY time DESC LIMIT :pageSize OFFSET :page * :pageSize")
    fun getAll(query: String, page: Int, pageSize: Int): Flow<List<Analytics>>

    @Delete
    suspend fun delete(entity: Analytics)

    @Query("DELETE FROM Analytics")
    suspend fun clear()

}
