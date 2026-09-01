package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.CommandResult
import io.github.yashkasera.alohomora.desktop.domain.model.Device
import io.github.yashkasera.alohomora.desktop.domain.model.WirelessDiscovery
import io.github.yashkasera.alohomora.desktop.domain.model.WirelessEndpoint
import kotlinx.coroutines.flow.StateFlow

interface AdbRepository {
    val devices: StateFlow<List<Device>>
    val selectedDeviceId: StateFlow<String?>
    val lastCommandResult: StateFlow<CommandResult?>
    val error: StateFlow<String?>

    fun refreshDevices()
    suspend fun activateDevice(deviceId: String, hostPort: Int, devicePort: Int): String?

    /** [deviceId] identifies whose forward to remove; null falls back to the port's owner. */
    suspend fun deactivateDevice(deviceId: String?, hostPort: Int): String?
    suspend fun enableTcpAndConnect(deviceId: String, host: String, tcpPort: Int): String?
    suspend fun disconnectHost(host: String, port: Int): String?

    /** Android 11+ wireless pairing. Returns null on success, else an error message. */
    suspend fun pairDevice(host: String, port: Int, code: String): String?

    /** `adb connect` to an already-paired wireless device. Returns null on success. */
    suspend fun connectWireless(host: String, port: Int): String?

    /** Discovers wireless-debugging endpoints on the LAN via mDNS; empty on failure. */
    suspend fun discoverWirelessEndpoints(): WirelessDiscovery

    /** The mDNS pairing endpoint whose service name matches [serviceName] (QR pairing), or null. */
    suspend fun findPairingEndpoint(serviceName: String): WirelessEndpoint?
    suspend fun restartServer(): String?
    suspend fun runCommandBlocking(deviceId: String?, args: List<String>): CommandResult
    fun runCommand(deviceId: String?, rawCommand: String)

    /** Starts a long-running command (e.g. screenrecord) without awaiting it. */
    suspend fun runDetached(deviceId: String?, args: List<String>): String?
    fun installApk(deviceId: String?, apkPath: String)
    fun uninstallPackage(deviceId: String?, packageName: String)
}
