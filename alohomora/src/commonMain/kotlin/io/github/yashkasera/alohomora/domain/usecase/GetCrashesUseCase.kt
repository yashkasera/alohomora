package io.github.yashkasera.alohomora.domain.usecase

import io.github.yashkasera.alohomora.data.entity.Crash
import io.github.yashkasera.alohomora.domain.repository.CrashRepository
import kotlinx.coroutines.flow.Flow

internal class GetCrashesUseCase(private val crashRepository: CrashRepository) {
    operator fun invoke(query: String = "", page: Int = 0, pageSize: Int = 100): Flow<List<Crash>> {
        return crashRepository.getAllCrashes(query, page, pageSize)
    }
}
