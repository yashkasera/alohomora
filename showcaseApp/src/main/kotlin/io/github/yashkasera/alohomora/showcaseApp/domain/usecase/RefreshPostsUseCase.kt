package io.github.yashkasera.alohomora.showcaseApp.domain.usecase

import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PostRepository

class RefreshPostsUseCase(
    private val repository: PostRepository,
) {
    suspend operator fun invoke() {
        repository.refreshPosts()
    }
}
