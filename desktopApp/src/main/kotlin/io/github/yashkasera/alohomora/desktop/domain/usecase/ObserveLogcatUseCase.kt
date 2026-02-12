package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.repository.LogcatRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveLogcatUseCase(
    private val repository: LogcatRepository,
) {
    operator fun invoke(): StateFlow<List<LogEntry>> = repository.entries
}
