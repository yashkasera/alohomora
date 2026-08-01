package io.github.yashkasera.alohomora.domain.usecase.traffic

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import kotlinx.coroutines.flow.Flow

internal class ObserveReplayResultUseCase(private val traceRepository: TrafficRepository) {
    operator fun invoke(sourceTraceId: String): Flow<TrafficEntry?> =
        traceRepository.observeReplayOf(sourceTraceId)
}
