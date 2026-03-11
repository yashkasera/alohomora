package io.github.yashkasera.alohomora.domain.usecase.crash

import io.github.yashkasera.alohomora.domain.repository.IncidentRepository

internal class ClearCrashesUseCase(private val crashRepository: IncidentRepository) {
    suspend operator fun invoke() {
        crashRepository.clearAll()
    }
}
