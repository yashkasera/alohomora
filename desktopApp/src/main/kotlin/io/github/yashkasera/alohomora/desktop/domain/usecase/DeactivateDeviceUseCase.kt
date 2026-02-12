package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository

class DeactivateDeviceUseCase(
    private val repository: AdbRepository,
) {
    suspend operator fun invoke(hostPort: Int): String? {
        return repository.deactivateDevice(hostPort)
    }
}
