package io.github.yashkasera.alohomora.desktop.data.adb

import io.github.yashkasera.alohomora.desktop.domain.model.CommandResult
import io.github.yashkasera.alohomora.desktop.domain.model.Device
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState

internal fun AdbDevice.toDomain(): Device = Device(
    id = id,
    state = state.toDomain(),
    model = model,
    product = product,
    transportId = transportId,
)

internal fun AdbDeviceState.toDomain(): DeviceState = when (this) {
    AdbDeviceState.DEVICE -> DeviceState.DEVICE
    AdbDeviceState.OFFLINE -> DeviceState.OFFLINE
    AdbDeviceState.UNAUTHORIZED -> DeviceState.UNAUTHORIZED
    AdbDeviceState.UNKNOWN -> DeviceState.UNKNOWN
}

internal fun AdbCommandResult.toDomain(): CommandResult = CommandResult(
    exitCode = exitCode,
    stdout = stdout,
    stderr = stderr,
)
