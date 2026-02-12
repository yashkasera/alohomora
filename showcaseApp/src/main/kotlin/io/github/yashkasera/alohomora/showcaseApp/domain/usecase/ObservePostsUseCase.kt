package io.github.yashkasera.alohomora.showcaseApp.domain.usecase

import io.github.yashkasera.alohomora.showcaseApp.domain.model.Post
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class ObservePostsUseCase(
    private val repository: PostRepository,
) {
    operator fun invoke(): Flow<List<Post>> = repository.observePosts()
}
