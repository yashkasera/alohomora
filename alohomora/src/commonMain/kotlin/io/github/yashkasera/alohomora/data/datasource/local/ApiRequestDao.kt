package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.yashkasera.alohomora.common.ApiRequest
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ApiRequestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ApiRequest): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(item: ApiRequest): Int

    @Query("SELECT id, status, host, path, `query`, method, duration, time, isViewed  FROM ApiRequest WHERE path LIKE '%' || :query || '%' AND method LIKE '%' || :method || '%' ORDER BY time DESC  LIMIT :pageSize OFFSET :page * :pageSize")
    fun getAll(query: String?, method: String?, page: Int, pageSize: Int): Flow<List<ApiRequest>>

    @Query("SELECT COUNT(*)  FROM ApiRequest WHERE path LIKE '%' || :query || '%' AND method LIKE '%' || :method || '%'")
    fun getCount(query: String?, method: String?): Flow<Long>

    @Query("SELECT id, status, host, path, `query`, method, isViewed FROM ApiRequest ORDER BY time DESC LIMIT 5")
    fun getLatest(): Flow<List<ApiRequest>>

    @Query("SELECT * FROM ApiRequest ORDER BY time DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<ApiRequest>

    @Query("SELECT * FROM ApiRequest WHERE id = :id")
    fun getById(id: String): Flow<ApiRequest?>

    @Delete
    suspend fun delete(entity: ApiRequest)

    @Query("DELETE FROM ApiRequest")
    suspend fun clear()


    @Query("Update ApiRequest SET isViewed = 1 WHERE id = :id")
    suspend fun markAsViewed(id: Long)
}
