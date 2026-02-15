package io.github.yashkasera.alohomora.domain.usecase.crash

import io.github.yashkasera.alohomora.domain.repository.CrashRepository

internal class ClearCrashesUseCase(private val crashRepository: CrashRepository) {
    suspend operator fun invoke() {
        crashRepository.clearAll()
    }
}
