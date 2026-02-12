package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.CommandResult
import io.github.yashkasera.alohomora.desktop.domain.model.Device
import kotlinx.coroutines.flow.StateFlow

interface AdbRepository {
    val devices: StateFlow<List<Device>>
    val selectedDeviceId: StateFlow<String?>
    val lastCommandResult: StateFlow<CommandResult?>
    val error: StateFlow<String?>

    fun refreshDevices()
    suspend fun activateDevice(deviceId: String, hostPort: Int, devicePort: Int): String?
    suspend fun deactivateDevice(hostPort: Int): String?
    fun runCommand(deviceId: String?, rawCommand: String)
    fun installApk(deviceId: String?, apkPath: String)
    fun uninstallPackage(deviceId: String?, packageName: String)
}
