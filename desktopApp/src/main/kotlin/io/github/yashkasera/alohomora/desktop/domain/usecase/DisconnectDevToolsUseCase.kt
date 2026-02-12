package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class DisconnectDevToolsUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke() {
        repository.disconnect()
    }
}
