package io.github.yashkasera.alohomora.showcaseApp.domain.repository

import io.github.yashkasera.alohomora.showcaseApp.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {
    fun observePosts(): Flow<List<Post>>
    suspend fun refreshPosts()
}
