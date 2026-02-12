package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.repository.LogcatRepository
import kotlinx.coroutines.flow.Flow

class StartLogcatUseCase(
    private val repository: LogcatRepository,
) {
    operator fun invoke(deviceId: String): Flow<LogEntry> {
        return repository.streamEntries(deviceId)
    }
}
