package io.github.yashkasera.alohomora.domain.usecase

import io.github.yashkasera.alohomora.domain.repository.CrashRepository

internal class MarkCrashAsViewedUseCase(private val crashRepository: CrashRepository) {
    suspend operator fun invoke(crashId: Long) {
        crashRepository.markAsViewed(crashId)
    }
}
