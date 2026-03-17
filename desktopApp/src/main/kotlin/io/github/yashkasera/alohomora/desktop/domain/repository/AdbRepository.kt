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
    suspend fun enableTcpAndConnect(deviceId: String, host: String, tcpPort: Int): String?
    suspend fun disconnectHost(host: String, port: Int): String?
    suspend fun restartServer(): String?
    suspend fun runCommandBlocking(deviceId: String?, args: List<String>): CommandResult
    fun runCommand(deviceId: String?, rawCommand: String)
    fun installApk(deviceId: String?, apkPath: String)
    fun uninstallPackage(deviceId: String?, packageName: String)
}
