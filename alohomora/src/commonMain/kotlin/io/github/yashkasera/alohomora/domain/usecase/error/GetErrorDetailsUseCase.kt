package io.github.yashkasera.alohomora.domain.usecase.error

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import kotlinx.coroutines.flow.Flow

internal class GetErrorDetailsUseCase(private val errorRepository: ErrorRepository) {
    operator fun invoke(errorId: Long): Flow<Error?> {
        return errorRepository.getById(errorId)
    }
}
