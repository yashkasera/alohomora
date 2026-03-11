package io.github.yashkasera.alohomora.domain.usecase.incident

import io.github.yashkasera.alohomora.domain.repository.IncidentRepository

internal class MarkIncidentAsViewedUseCase(private val incidentRepository: IncidentRepository) {
    suspend operator fun invoke(incidentId: Long) {
        incidentRepository.markAsViewed(incidentId)
    }
}
