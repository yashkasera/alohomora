package io.github.yashkasera.alohomora.domain.usecase.incident

import io.github.yashkasera.alohomora.domain.repository.IncidentRepository

internal class ClearIncidentsUseCase(private val incidentRepository: IncidentRepository) {
    suspend operator fun invoke() {
        incidentRepository.clearAll()
    }
}
