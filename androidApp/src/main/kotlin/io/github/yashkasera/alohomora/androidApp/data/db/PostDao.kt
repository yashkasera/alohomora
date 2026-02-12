package io.github.yashkasera.alohomora.androidApp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY id")
    fun observePosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(posts: List<PostEntity>) {
        clear()
        upsert(posts)
    }
}
