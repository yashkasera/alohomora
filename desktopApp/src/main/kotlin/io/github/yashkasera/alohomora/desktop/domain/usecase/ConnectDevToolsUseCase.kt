package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class ConnectDevToolsUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(host: String, port: Int) {
        repository.connect(host, port)
    }
}
