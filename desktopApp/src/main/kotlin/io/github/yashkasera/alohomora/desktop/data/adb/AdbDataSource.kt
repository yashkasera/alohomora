package io.github.yashkasera.alohomora.desktop.data.adb

interface AdbDataSource {
    suspend fun listDevices(): List<AdbDevice>
    suspend fun forwardDevToolsPort(deviceId: String, hostPort: Int, devicePort: Int)
    suspend fun removeForward(deviceId: String, hostPort: Int)
    suspend fun runCommand(deviceId: String?, args: List<String>): AdbCommandResult
}
