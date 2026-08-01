package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.replay.ReplayRequest

class ReplayTraceUseCase(private val repository: DevToolsRepository) {
    operator fun invoke(request: ReplayRequest) = repository.replayTrace(request)
}
