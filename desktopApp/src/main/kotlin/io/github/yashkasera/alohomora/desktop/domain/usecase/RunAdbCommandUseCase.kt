package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository

class RunAdbCommandUseCase(
    private val repository: AdbRepository,
) {
    operator fun invoke(deviceId: String?, rawCommand: String) {
        repository.runCommand(deviceId, rawCommand)
    }
}
