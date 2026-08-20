package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestCacheDeleteUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(storeName: String, key: String) {
        repository.requestCacheDelete(storeName, key)
    }
}
