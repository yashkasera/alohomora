package io.github.yashkasera.alohomora.domain.usecase.trace

import io.github.yashkasera.alohomora.domain.repository.TraceRepository

/**
 * Marks a trace as read so the list can dim it.
 *
 * The tint, the `isViewed` column and `TraceRepository.markAsViewed` all already existed — nothing
 * ever called the last one, so the flag stayed false forever and the styling never appeared. This
 * is the missing link, mirroring
 * [io.github.yashkasera.alohomora.domain.usecase.incident.MarkIncidentAsViewedUseCase].
 */
internal class MarkTraceAsViewedUseCase(private val traceRepository: TraceRepository) {
    suspend operator fun invoke(traceId: String) {
        traceRepository.markAsViewed(traceId)
    }
}
