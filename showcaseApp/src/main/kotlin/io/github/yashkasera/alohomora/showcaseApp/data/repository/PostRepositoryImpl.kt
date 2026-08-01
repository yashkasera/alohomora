package io.github.yashkasera.alohomora.showcaseApp.data.repository

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.showcaseApp.data.api.PostsApi
import io.github.yashkasera.alohomora.showcaseApp.data.db.PostDao
import io.github.yashkasera.alohomora.showcaseApp.data.db.PostEntity
import io.github.yashkasera.alohomora.showcaseApp.domain.model.Post
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PostRepository
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostRepositoryImpl(
    private val api: PostsApi,
    private val postDao: PostDao,
    private val preferencesRepository: PreferencesRepository,
) : PostRepository {

    override fun observePosts(): Flow<List<Post>> {
        return postDao.observePosts().map { entities ->
            entities.map { entity ->
                Post(
                    id = entity.id,
                    userId = entity.userId,
                    title = entity.title,
                    body = entity.body
                )
            }
        }
    }

    override suspend fun refreshPosts() {
        Alohomora.recordEvent("posts_refresh_start")
        try {
            val now = System.currentTimeMillis()
            val posts = api.fetchPosts().map { dto ->
                PostEntity(
                    id = dto.id,
                    userId = dto.userId,
                    title = dto.title,
                    body = dto.body,
                    updatedAtEpochMillis = now
                )
            }
            postDao.replaceAll(posts)
            preferencesRepository.updateLastRefreshEpochMillis(now)
            Alohomora.recordEvent(
                "posts_refresh_success",
                mapOf("count" to posts.size.toString())
            )
        } catch (e: Exception) {
            Alohomora.recordEvent(
                "posts_refresh_failure",
                mapOf("error" to (e.message ?: "unknown"))
            )
            throw e
        }
    }
}
