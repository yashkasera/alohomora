package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestCacheRefreshUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke() {
        repository.requestCacheRefresh()
    }
}
