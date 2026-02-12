package io.github.yashkasera.alohomora.desktop.data.adb

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

    override fun refreshDevices() {
        scope.launch {
            try {
                _devices.value = dataSource.listDevices().map { it.toDomain() }
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to list devices"
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

            val previousId = _selectedDeviceId.value
            return@withLock try {
                if (!previousId.isNullOrBlank() && previousId != deviceId) {
                    dataSource.removeForward(previousId, hostPort)
                }
                dataSource.forwardDevToolsPort(deviceId, hostPort, devicePort)
                _selectedDeviceId.value = deviceId
                _error.value = null
                null
            } catch (e: Exception) {
                _error.value = e.message
                e.message ?: "Failed to activate device"
            }
        }
    }

    override suspend fun deactivateDevice(hostPort: Int): String? {
        return deviceMutex.withLock {
            val deviceId = _selectedDeviceId.value
            if (deviceId.isNullOrBlank()) return@withLock null
            return@withLock try {
                dataSource.removeForward(deviceId, hostPort)
                _selectedDeviceId.value = null
                null
            } catch (e: Exception) {
                _error.value = e.message
                e.message ?: "Failed to remove forward"
            }
        }
    }

    override fun runCommand(deviceId: String?, rawCommand: String) {
        scope.launch {
            val args = rawCommand.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (args.isEmpty()) return@launch
            val result = dataSource.runCommand(deviceId, args)
            _lastCommandResult.value = result.toDomain()
        }
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
