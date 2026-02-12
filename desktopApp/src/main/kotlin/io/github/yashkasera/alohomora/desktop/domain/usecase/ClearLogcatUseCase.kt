package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.LogcatRepository

class ClearLogcatUseCase(
    private val repository: LogcatRepository,
) {
    operator fun invoke() {
        repository.clear()
    }
}
