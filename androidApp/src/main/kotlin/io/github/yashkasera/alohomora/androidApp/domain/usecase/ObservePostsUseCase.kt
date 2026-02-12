package io.github.yashkasera.alohomora.androidApp.domain.usecase

import io.github.yashkasera.alohomora.androidApp.domain.model.Post
import io.github.yashkasera.alohomora.androidApp.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow

class ObservePostsUseCase(
    private val repository: PostRepository,
) {
    operator fun invoke(): Flow<List<Post>> = repository.observePosts()
}
