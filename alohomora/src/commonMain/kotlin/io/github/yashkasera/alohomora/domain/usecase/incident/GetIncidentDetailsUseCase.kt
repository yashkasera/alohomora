package io.github.yashkasera.alohomora.domain.usecase.incident

import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow

internal class GetIncidentDetailsUseCase(private val incidentRepository: IncidentRepository) {
    operator fun invoke(incidentId: Long): Flow<Incident?> {
        return incidentRepository.getById(incidentId)
    }
}
