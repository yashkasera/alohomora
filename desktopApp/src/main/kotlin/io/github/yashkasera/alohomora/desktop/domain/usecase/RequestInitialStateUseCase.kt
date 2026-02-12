package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestInitialStateUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke() {
        repository.requestInitialState()
    }
}
