package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestCacheUpdateUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(storeName: String, key: String, newValue: String?, type: String) {
        repository.requestCacheUpdate(storeName, key, newValue, type)
    }
}
