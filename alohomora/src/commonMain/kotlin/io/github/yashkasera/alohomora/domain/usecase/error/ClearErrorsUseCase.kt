package io.github.yashkasera.alohomora.domain.usecase.error

import io.github.yashkasera.alohomora.domain.repository.ErrorRepository

internal class ClearErrorsUseCase(private val errorRepository: ErrorRepository) {
    suspend operator fun invoke() {
        errorRepository.clearAll()
    }
}
