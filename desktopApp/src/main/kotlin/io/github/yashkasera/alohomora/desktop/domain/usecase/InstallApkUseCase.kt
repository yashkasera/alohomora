package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository

class InstallApkUseCase(
    private val repository: AdbRepository,
) {
    operator fun invoke(deviceId: String?, apkPath: String) {
        repository.installApk(deviceId, apkPath)
    }
}
