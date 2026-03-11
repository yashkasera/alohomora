package io.github.yashkasera.alohomora.domain.usecase.crash

import io.github.yashkasera.alohomora.domain.repository.IncidentRepository

internal class MarkCrashAsViewedUseCase(private val crashRepository: IncidentRepository) {
    suspend operator fun invoke(crashId: Long) {
        crashRepository.markAsViewed(crashId)
    }
}
