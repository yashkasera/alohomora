package io.github.yashkasera.alohomora.domain.usecase.trace

import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.domain.repository.TraceRepository
import kotlinx.coroutines.flow.Flow

internal class GetTraceDetailsUseCase(private val traceRepository: TraceRepository) {
    operator fun invoke(id: String): Flow<TraceEntry?> {
        return traceRepository.getById(id)
    }
}
