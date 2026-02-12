package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository

class SelectDeviceUseCase(
    private val repository: AdbRepository,
) {
    suspend operator fun invoke(deviceId: String, hostPort: Int, devicePort: Int): String? {
        return repository.activateDevice(deviceId, hostPort, devicePort)
    }
}
