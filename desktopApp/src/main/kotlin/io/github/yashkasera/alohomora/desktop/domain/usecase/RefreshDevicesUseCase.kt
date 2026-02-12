package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository

class RefreshDevicesUseCase(
    private val repository: AdbRepository,
) {
    operator fun invoke() = repository.refreshDevices()
}
