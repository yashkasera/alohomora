package io.github.yashkasera.alohomora.domain.usecase.crash

import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.domain.repository.IncidentRepository
import kotlinx.coroutines.flow.Flow

internal class GetCrashDetailsUseCase(private val crashRepository: IncidentRepository) {
    operator fun invoke(crashId: Long): Flow<Incident?> {
        return crashRepository.getCrashById(crashId)
    }
}
