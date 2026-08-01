package io.github.yashkasera.alohomora.domain.usecase.traffic

import io.github.yashkasera.alohomora.domain.repository.TrafficRepository

/**
 * Marks a trace as read so the list can dim it.
 *
 * The tint, the `isViewed` column and `TrafficRepository.markAsViewed` all already existed — nothing
 * ever called the last one, so the flag stayed false forever and the styling never appeared. This
 * is the missing link, mirroring
 * [io.github.yashkasera.alohomora.domain.usecase.error.MarkErrorAsViewedUseCase].
 */
internal class MarkTrafficAsViewedUseCase(private val traceRepository: TrafficRepository) {
    suspend operator fun invoke(traceId: String) {
        traceRepository.markAsViewed(traceId)
    }
}
