package io.github.yashkasera.alohomora.androidApp.domain.repository

import io.github.yashkasera.alohomora.androidApp.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observePosts(): Flow<List<Post>>
    suspend fun refreshPosts()
}
