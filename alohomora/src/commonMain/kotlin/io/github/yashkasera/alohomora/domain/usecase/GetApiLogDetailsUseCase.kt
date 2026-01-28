package io.github.yashkasera.alohomora.domain.usecase

import io.github.yashkasera.alohomora.data.entity.ApiRequest
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow

internal class GetApiLogDetailsUseCase(private val networkRepository: NetworkRepository) {
    operator fun invoke(id: String): Flow<ApiRequest?> {
        return networkRepository.getById(id)
    }
}
