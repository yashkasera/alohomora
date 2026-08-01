package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class SwitchDevToolsDeviceUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(target: DevToolsTarget, deviceId: String? = null) {
        repository.switchDevice(target, deviceId)
    }

    operator fun invoke(host: String, port: Int, deviceId: String? = null) {
        repository.switchDevice(DevToolsTarget.Tcp(host, port), deviceId)
    }
}
