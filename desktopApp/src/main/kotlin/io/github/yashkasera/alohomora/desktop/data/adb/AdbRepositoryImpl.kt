package io.github.yashkasera.alohomora.desktop.data.adb

import io.github.yashkasera.alohomora.desktop.data.ios.IosDeviceDataSource
import io.github.yashkasera.alohomora.desktop.domain.model.CommandResult
import io.github.yashkasera.alohomora.desktop.domain.model.Device
import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AdbRepositoryImpl(
    private val dataSource: AdbDataSource = AdbServiceImpl(),
    /**
     * iOS discovery, alongside Android.
     *
     * Deliberately merged into one device list rather than split into a separate repository:
     * the DevTools protocol is identical across platforms, so only *discovery* and *transport*
     * differ. Keeping one list means the launcher, session model and every panel stay
     * platform-agnostic, with [io.github.yashkasera.alohomora.desktop.domain.model.Device.platform]
     * driving capability gating.
     */
    private val iosDataSource: IosDeviceDataSource? = IosDeviceDataSource(),
) : AdbRepository {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val deviceMutex = Mutex()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    override val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    override val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    private val _lastCommandResult = MutableStateFlow<CommandResult?>(null)
    override val lastCommandResult: StateFlow<CommandResult?> = _lastCommandResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Live port forwards, keyed by device id.
     *
     * Multi-device support was broken without this. `activateDevice` used to tear down the
     * *previous* device's forward on the same host port, and `deactivateDevice` resolved the
     * device from the single global [_selectedDeviceId] — so with two windows open, connecting
     * device B killed device A's tunnel, and closing window A removed device B's forward.
     * Guarded by [deviceMutex] along with every other mutation here.
     */
    private val forwards = mutableMapOf<String, Int>()

    override fun refreshDevices() {
        scope.launch {
            // Android and iOS are collected independently so one failing toolchain does not
            // hide the other's devices — a Mac without the Android SDK should still list
            // iPhones, and a machine with no Xcode should still list Android devices.
            val android = runCatching { dataSource.listDevices().map { it.toDomain() } }
            val ios = runCatching { iosDataSource?.listDevices().orEmpty() }

            _devices.value = android.getOrDefault(emptyList()) + ios.getOrDefault(emptyList())

            _error.value = when {
                android.isFailure && ios.isFailure ->
                    android.exceptionOrNull()?.message ?: "Failed to list devices"
                // Only surface a partial failure when the other side found nothing, otherwise
                // a missing SDK would nag on every 3-second poll.
                android.isFailure && _devices.value.isEmpty() -> android.exceptionOrNull()?.message
                else -> null
            }
        }
    }

    override suspend fun activateDevice(deviceId: String, hostPort: Int, devicePort: Int): String? {
        return deviceMutex.withLock {
            val device = _devices.value.firstOrNull { it.id == deviceId }
            if (device == null) return@withLock "Device not found"
            if (device.state != io.github.yashkasera.alohomora.desktop.domain.model.DeviceState.DEVICE) {
                return@withLock "Device ${device.id} is ${device.state.name.lowercase()}"
            }

            return@withLock try {
                // Only this device's own stale forward is replaced. Other devices' forwards
                // are on their own host ports and must be left alone.
                forwards[deviceId]?.takeIf { it != hostPort }?.let { stalePort ->
                    runCatching { dataSource.removeForward(deviceId, stalePort) }
                }
                dataSource.forwardDevToolsPort(deviceId, hostPort, devicePort)
                forwards[deviceId] = hostPort
                _selectedDeviceId.value = deviceId
                _error.value = null
                null
            } catch (e: Exception) {
                _error.value = e.message
                e.message ?: "Failed to activate device"
            }
        }
    }

    override suspend fun deactivateDevice(deviceId: String?, hostPort: Int): String? {
        return deviceMutex.withLock {
            // Resolve by the caller's own device id, falling back to whichever device actually
            // owns this host port. Never the global selection — that is what made closing one
            // window tear down a different window's tunnel.
            val target = deviceId?.takeIf { it.isNotBlank() }
                ?: forwards.entries.firstOrNull { it.value == hostPort }?.key
                ?: return@withLock null
            return@withLock try {
                dataSource.removeForward(target, hostPort)
                forwards.remove(target)
                if (_selectedDeviceId.value == target) _selectedDeviceId.value = null
                null
            } catch (e: Exception) {
                _error.value = e.message
                e.message ?: "Failed to remove forward"
            }
        }
    }

    override suspend fun enableTcpAndConnect(
        deviceId: String,
        host: String,
        tcpPort: Int,
    ): String? {
        return deviceMutex.withLock {
            return@withLock try {
                dataSource.enableTcpMode(deviceId, tcpPort)
                val result = dataSource.connect(host, tcpPort)
                if (result.exitCode != 0) {
                    val message =
                        result.stderr.ifBlank { result.stdout.ifBlank { "adb connect failed" } }
                    _error.value = message
                    return@withLock message
                }
                _error.value = null
                null
            } catch (e: Exception) {
                _error.value = e.message
                e.message ?: "Failed to enable tcp and connect"
            }
        }
    }

    override suspend fun disconnectHost(host: String, port: Int): String? {
        return try {
            val result = dataSource.disconnect(host, port)
            if (result.exitCode != 0) {
                val message =
                    result.stderr.ifBlank { result.stdout.ifBlank { "adb disconnect failed" } }
                _error.value = message
                message
            } else {
                _error.value = null
                null
            }
        } catch (e: Exception) {
            _error.value = e.message
            e.message ?: "Failed to disconnect host"
        }
    }

    override suspend fun restartServer(): String? {
        return try {
            val result = dataSource.restartServer()
            if (result.exitCode != 0) {
                val message =
                    result.stderr.ifBlank { result.stdout.ifBlank { "adb restart failed" } }
                _error.value = message
                message
            } else {
                _error.value = null
                null
            }
        } catch (e: Exception) {
            _error.value = e.message
            e.message ?: "Failed to restart adb"
        }
    }

    override suspend fun runCommandBlocking(deviceId: String?, args: List<String>): CommandResult {
        val result = dataSource.runCommand(deviceId, args).toDomain()
        _lastCommandResult.value = result
        return result
    }

    override fun runCommand(deviceId: String?, rawCommand: String) {
        scope.launch {
            val args = rawCommand.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (args.isEmpty()) return@launch
            val result = dataSource.runCommand(deviceId, args)
            _lastCommandResult.value = result.toDomain()
        }
    }

    override suspend fun runDetached(deviceId: String?, args: List<String>): String? {
        val error = dataSource.runDetached(deviceId, args)
        if (error != null) _error.value = error
        return error
    }

    override fun installApk(deviceId: String?, apkPath: String) {
        if (apkPath.isBlank()) return
        scope.launch {
            val result = dataSource.runCommand(deviceId, listOf("install", "-r", apkPath))
            _lastCommandResult.value = result.toDomain()
            if (result.exitCode != 0) {
                _error.value = result.stderr.ifBlank { "Install failed" }
            } else {
                _error.value = null
            }
        }
    }

    override fun uninstallPackage(deviceId: String?, packageName: String) {
        if (packageName.isBlank()) return
        scope.launch {
            val result = dataSource.runCommand(deviceId, listOf("uninstall", packageName))
            _lastCommandResult.value = result.toDomain()
            if (result.exitCode != 0) {
                _error.value = result.stderr.ifBlank { "Uninstall failed" }
            } else {
                _error.value = null
            }
        }
    }
}
