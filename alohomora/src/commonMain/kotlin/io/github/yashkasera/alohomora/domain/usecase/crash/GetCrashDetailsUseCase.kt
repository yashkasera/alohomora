package io.github.yashkasera.alohomora.domain.usecase.crash

import io.github.yashkasera.alohomora.common.Crash
import io.github.yashkasera.alohomora.domain.repository.CrashRepository
import kotlinx.coroutines.flow.Flow

internal class GetCrashDetailsUseCase(private val crashRepository: CrashRepository) {
    operator fun invoke(crashId: Long): Flow<Crash?> {
        return crashRepository.getCrashById(crashId)
    }
}
