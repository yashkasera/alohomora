package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository

class UninstallPackageUseCase(
    private val repository: AdbRepository,
) {
    operator fun invoke(deviceId: String?, packageName: String) {
        repository.uninstallPackage(deviceId, packageName)
    }
}
