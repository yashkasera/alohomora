package io.github.yashkasera.alohomora.androidApp.domain.usecase

import io.github.yashkasera.alohomora.androidApp.domain.repository.PostRepository

class RefreshPostsUseCase(
    private val repository: PostRepository,
) {
    suspend operator fun invoke() {
        repository.refreshPosts()
    }
}
