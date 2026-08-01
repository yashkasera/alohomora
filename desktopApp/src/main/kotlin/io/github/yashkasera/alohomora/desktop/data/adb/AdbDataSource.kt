package io.github.yashkasera.alohomora.desktop.data.adb

interface AdbDataSource {
    suspend fun listDevices(): List<AdbDevice>
    suspend fun forwardDevToolsPort(deviceId: String, hostPort: Int, devicePort: Int)
    suspend fun removeForward(deviceId: String, hostPort: Int)
    suspend fun enableTcpMode(deviceId: String, tcpPort: Int)
    suspend fun connect(host: String, port: Int): AdbCommandResult
    suspend fun disconnect(host: String, port: Int): AdbCommandResult
    suspend fun restartServer(): AdbCommandResult
    suspend fun runCommand(deviceId: String?, args: List<String>): AdbCommandResult

    /** Starts a long-running command (e.g. screenrecord) without awaiting it. */
    suspend fun runDetached(deviceId: String?, args: List<String>): String?
}