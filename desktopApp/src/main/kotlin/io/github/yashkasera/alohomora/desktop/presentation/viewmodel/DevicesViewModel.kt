package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.model.CommandResult
import io.github.yashkasera.alohomora.desktop.domain.model.Device
import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.DeactivateDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.InstallApkUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RefreshDevicesUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RunAdbCommandUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SelectDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.UninstallPackageUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DevicesViewModel(
    private val repository: AdbRepository,
    private val refreshDevicesUseCase: RefreshDevicesUseCase,
    private val selectDeviceUseCase: SelectDeviceUseCase,
    private val deactivateDeviceUseCase: DeactivateDeviceUseCase,
    private val runAdbCommandUseCase: RunAdbCommandUseCase,
    private val installApkUseCase: InstallApkUseCase,
    private val uninstallPackageUseCase: UninstallPackageUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val devices: StateFlow<List<DeviceUi>> = repository.devices
        .map { devices -> devices.map { it.toUi() } }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val selectedDeviceId: StateFlow<String?> = repository.selectedDeviceId
    val lastCommandResult: StateFlow<CommandResult?> = repository.lastCommandResult
    val error: StateFlow<String?> = repository.error

    private val _activating = MutableStateFlow(false)
    val activating: StateFlow<Boolean> = _activating.asStateFlow()

    fun refreshDevices() = refreshDevicesUseCase()

    fun selectDevice(deviceId: String, hostPort: Int, devicePort: Int, onError: (String?) -> Unit = {}) {
        scope.launch {
            _activating.value = true
            val error = selectDeviceUseCase(deviceId, hostPort, devicePort)
            _activating.value = false
            onError(error)
        }
    }

    fun deactivateDevice(hostPort: Int, onError: (String?) -> Unit = {}) {
        scope.launch {
            val error = deactivateDeviceUseCase(hostPort)
            onError(error)
        }
    }

    fun runCommand(deviceId: String?, rawCommand: String) {
        runAdbCommandUseCase(deviceId, rawCommand)
    }

    fun installApk(deviceId: String?, apkPath: String) {
        installApkUseCase(deviceId, apkPath)
    }

    fun uninstallPackage(deviceId: String?, packageName: String) {
        uninstallPackageUseCase(deviceId, packageName)
    }

    private fun Device.toUi(): DeviceUi = DeviceUi(
        id = id,
        state = state,
        model = model,
    )
}
