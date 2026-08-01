package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestCacheValueUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(key: String) {
        repository.requestCacheValue(key)
    }
}
