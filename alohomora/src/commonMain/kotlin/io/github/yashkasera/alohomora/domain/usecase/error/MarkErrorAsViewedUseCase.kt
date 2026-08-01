package io.github.yashkasera.alohomora.domain.usecase.error

import io.github.yashkasera.alohomora.domain.repository.ErrorRepository

internal class MarkErrorAsViewedUseCase(private val errorRepository: ErrorRepository) {
    suspend operator fun invoke(errorId: Long) {
        errorRepository.markAsViewed(errorId)
    }
}
