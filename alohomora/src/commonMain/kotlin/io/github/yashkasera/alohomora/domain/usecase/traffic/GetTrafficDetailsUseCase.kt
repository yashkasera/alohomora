package io.github.yashkasera.alohomora.domain.usecase.traffic

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import kotlinx.coroutines.flow.Flow

internal class GetTrafficDetailsUseCase(private val traceRepository: TrafficRepository) {
    operator fun invoke(id: String): Flow<TrafficEntry?> {
        return traceRepository.getById(id)
    }
}
