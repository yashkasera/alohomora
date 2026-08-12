package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import androidx.compose.material3.SnackbarHostState
import io.github.yashkasera.alohomora.desktop.domain.model.CommandResult
import io.github.yashkasera.alohomora.desktop.domain.model.Device
import io.github.yashkasera.alohomora.desktop.domain.repository.AdbRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.DeactivateDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.InstallApkUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RefreshDevicesUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RunAdbCommandUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.SelectDeviceUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.UninstallPackageUseCase
import io.github.yashkasera.alohomora.desktop.data.local.DeepLinkEntry
import io.github.yashkasera.alohomora.desktop.data.local.DeepLinkHistoryStore
import io.github.yashkasera.alohomora.desktop.presentation.model.AdbCommandLogEntry
import io.github.yashkasera.alohomora.desktop.presentation.model.DashboardUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private val deepLinkHistoryStore: DeepLinkHistoryStore = DeepLinkHistoryStore(),
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val snackbarHostState = SnackbarHostState()

    private val _deepLinkHistory = MutableStateFlow<List<DeepLinkEntry>>(emptyList())
    val deepLinkHistory: StateFlow<List<DeepLinkEntry>> = _deepLinkHistory.asStateFlow()

    init {
        scope.launch { _deepLinkHistory.value = deepLinkHistoryStore.load() }
    }

    val devices: StateFlow<List<DeviceUi>> = repository.devices
        .map { devices -> devices.map { it.toUi() } }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, emptyList())

    val selectedDeviceId: StateFlow<String?> = repository.selectedDeviceId
    val error: StateFlow<String?> = repository.error

    private val _adbCommandHistory = MutableStateFlow<List<AdbCommandLogEntry>>(emptyList())
    val adbCommandHistory: StateFlow<List<AdbCommandLogEntry>> = _adbCommandHistory.asStateFlow()

    private val _wifiEnabled = MutableStateFlow<Boolean?>(null)
    val wifiEnabled: StateFlow<Boolean?> = _wifiEnabled.asStateFlow()

    private val _dataEnabled = MutableStateFlow<Boolean?>(null)
    val dataEnabled: StateFlow<Boolean?> = _dataEnabled.asStateFlow()

    private val _dashboardState = MutableStateFlow(DashboardUiState())
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private var dashboardPollingJob: Job? = null

    fun refreshDevices() = refreshDevicesUseCase()

    fun selectDevice(
        deviceId: String,
        hostPort: Int,
        devicePort: Int,
        onError: (String?) -> Unit = {},
    ) {
        scope.launch {
            val error = selectDeviceUseCase(deviceId, hostPort, devicePort)
            onError(error)
        }
    }

    fun deactivateDevice(deviceId: String?, hostPort: Int, onError: (String?) -> Unit = {}) {
        scope.launch {
            val error = deactivateDeviceUseCase(deviceId, hostPort)
            onError(error)
        }
    }

    fun connectOverTcp(deviceId: String, host: String, port: Int, onError: (String?) -> Unit = {}) {
        scope.launch {
            val error = repository.enableTcpAndConnect(deviceId, host, port)
            onError(error)
        }
    }

    fun disconnectHost(host: String, port: Int, onError: (String?) -> Unit = {}) {
        scope.launch {
            val error = repository.disconnectHost(host, port)
            onError(error)
        }
    }

    fun restartAdb(onError: (String?) -> Unit = {}) {
        scope.launch {
            logAdbCommand(null, "adb kill-server")
            logAdbCommand(null, "adb start-server")
            val error = repository.restartServer()
            onError(error)
        }
    }

    fun startDashboardPolling(deviceId: String?, packageName: String?) {
        if (deviceId.isNullOrBlank()) {
            _dashboardState.value = DashboardUiState()
            dashboardPollingJob?.cancel()
            dashboardPollingJob = null
            return
        }
        dashboardPollingJob?.cancel()
        dashboardPollingJob = scope.launch {
            while (true) {
                _dashboardState.value =
                    _dashboardState.value.copy(loadingMetrics = true)
                val release = repository.runCommandBlocking(
                    deviceId,
                    listOf("shell", "getprop", "ro.build.version.release"),
                ).stdout.trim()
                val api = repository.runCommandBlocking(
                    deviceId,
                    listOf("shell", "getprop", "ro.build.version.sdk"),
                ).stdout.trim()
                val batteryDump = repository.runCommandBlocking(
                    deviceId,
                    listOf("shell", "dumpsys", "battery"),
                ).stdout
                val memDump = repository.runCommandBlocking(
                    deviceId,
                    listOf("shell", "cat", "/proc/meminfo"),
                ).stdout
                val cpuDump = repository.runCommandBlocking(
                    deviceId,
                    listOf("shell", "dumpsys", "cpuinfo"),
                ).stdout
                val gfxDump = if (!packageName.isNullOrBlank()) {
                    repository.runCommandBlocking(
                        deviceId,
                        listOf("shell", "dumpsys", "gfxinfo", packageName),
                    ).stdout
                } else {
                    ""
                }

                val batteryLevel =
                    Regex("level:\\s*(\\d+)").find(batteryDump)?.groupValues?.get(1) ?: "-"
                val batteryStatusCode =
                    Regex("status:\\s*(\\d+)").find(batteryDump)?.groupValues?.get(1)
                val batteryStatus = when (batteryStatusCode) {
                    "2" -> "Charging"
                    "3" -> "Discharging"
                    "5" -> "Full"
                    else -> "Unknown"
                }

                val memTotalKb = Regex("MemTotal:\\s*(\\d+)").find(memDump)?.groupValues?.get(1)
                    ?.toDoubleOrNull() ?: 0.0
                val memAvailKb = Regex("MemAvailable:\\s*(\\d+)").find(memDump)?.groupValues?.get(1)
                    ?.toDoubleOrNull() ?: 0.0
                val usedGb = ((memTotalKb - memAvailKb) / 1024.0 / 1024.0).coerceAtLeast(0.0)
                val totalGb = (memTotalKb / 1024.0 / 1024.0).coerceAtLeast(0.0)

                val totalLine = cpuDump.lines().firstOrNull { it.trimStart().startsWith("TOTAL") }
                val cpuUsage = Regex("(\\d+)%").find(totalLine ?: "")?.groupValues?.get(1)
                    ?: Regex("(\\d+)%").find(cpuDump)?.groupValues?.get(1)
                    ?: "-"

                val jankyFrames =
                    Regex("Janky frames:\\s*(\\d+)").find(gfxDump)?.groupValues?.get(1) ?: "-"
                val frameTime =
                    Regex("50th percentile:\\s*([0-9.]+)ms").find(gfxDump)?.groupValues?.get(1)
                val frameRate = frameTime?.toDoubleOrNull()?.let { ms ->
                    if (ms > 0) (1000.0 / ms).roundToInt().toString() else "-"
                } ?: "-"

                _dashboardState.value = _dashboardState.value.copy(
                    androidVersion = release.ifBlank { "-" },
                    apiLevel = api.ifBlank { "-" },
                    batteryPercent = if (batteryLevel == "-") "-" else "$batteryLevel%",
                    batteryStatus = batteryStatus,
                    memoryUsageGb = if (usedGb > 0) "%.1f".format(usedGb) else "-",
                    memoryTotalGb = if (totalGb > 0) "%.1f".format(totalGb) else "-",
                    cpuUsagePercent = if (cpuUsage == "-") "-" else "$cpuUsage%",
                    networkMbPerSec = "0.0",
                    latencyMs = estimateLatencyMs(deviceId),
                    frameRateFps = frameRate,
                    frameTimeMs = frameTime ?: "-",
                    jankFrames = jankyFrames,
                    loadingMetrics = false,
                )
                delay(3000)
            }
        }
    }

    fun setActionMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun setActionError(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    private suspend fun estimateLatencyMs(deviceId: String): String {
        val output = repository.runCommandBlocking(
            deviceId,
            listOf("shell", "ping", "-c", "1", "8.8.8.8"),
        ).stdout
        val ms = Regex("time=([0-9.]+)").find(output)?.groupValues?.get(1)?.toDoubleOrNull()
        return if (ms != null) "${ms.roundToInt()}" else "-"
    }

    fun runCommand(deviceId: String?, rawCommand: String) {
        if (rawCommand.isBlank()) return
        logAdbCommand(deviceId, formatCommand(deviceId, rawCommand))
        runAdbCommandUseCase(deviceId, rawCommand)
    }

    fun installApk(deviceId: String?, apkPath: String) {
        if (apkPath.isBlank()) {
            setActionError("Select an APK first")
            return
        }
        logAdbCommand(deviceId, formatCommand(deviceId, "install -r $apkPath"))
        installApkUseCase(deviceId, apkPath)
    }

    fun uninstallPackage(deviceId: String?, packageName: String) {
        if (packageName.isBlank()) {
            setActionError("Enter a package name")
            return
        }
        logAdbCommand(deviceId, formatCommand(deviceId, "uninstall $packageName"))
        uninstallPackageUseCase(deviceId, packageName)
    }

    fun toggleWifi(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        scope.launch {
            val stateResult = runLoggedBlocking(deviceId, listOf("shell", "dumpsys", "wifi"))
            val stateLine = stateResult.stdout.lines().firstOrNull { it.contains("Wi-Fi is") }
            if (stateLine == null) {
                setActionError("Unable to determine Wi-Fi state")
                return@launch
            }
            when {
                stateLine.contains("enabled", ignoreCase = true) -> {
                    _wifiEnabled.value = false
                    runLoggedBlocking(deviceId, listOf("shell", "svc", "wifi", "disable"))
                    setActionMessage("Wi-Fi disabled")
                }

                stateLine.contains("disabled", ignoreCase = true) -> {
                    _wifiEnabled.value = true
                    runLoggedBlocking(deviceId, listOf("shell", "svc", "wifi", "enable"))
                    setActionMessage("Wi-Fi enabled")
                }

                else -> setActionError("Unable to determine Wi-Fi state")
            }
            delay(500)
            refreshConnectivityState(deviceId)
        }
    }

    fun toggleMobileData(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        scope.launch {
            val stateResult =
                runLoggedBlocking(deviceId, listOf("shell", "dumpsys", "telephony.registry"))
            val stateLine =
                stateResult.stdout.lines().firstOrNull { it.contains("mDataConnectionState") }
            if (stateLine == null) {
                setActionError("Unable to determine mobile data state")
                return@launch
            }
            val connected = stateLine.contains("CONNECTED", ignoreCase = true) ||
                Regex("mDataConnectionState=(\\d+)").find(stateLine)?.groupValues?.get(1) == "2"
            if (connected) {
                _dataEnabled.value = false
                runLoggedBlocking(deviceId, listOf("shell", "svc", "data", "disable"))
                setActionMessage("Mobile data disabled")
            } else {
                _dataEnabled.value = true
                runLoggedBlocking(deviceId, listOf("shell", "svc", "data", "enable"))
                setActionMessage("Mobile data enabled")
            }
            delay(500)
            refreshConnectivityState(deviceId)
        }
    }

    fun refreshConnectivityState(deviceId: String?) {
        if (deviceId.isNullOrBlank()) {
            _wifiEnabled.value = null
            _dataEnabled.value = null
            return
        }
        scope.launch {
            val wifiResult = runLoggedBlocking(deviceId, listOf("shell", "dumpsys", "wifi"))
            val wifiLine = wifiResult.stdout.lines().firstOrNull { it.contains("Wi-Fi is") }
            _wifiEnabled.value = when {
                wifiLine?.contains("enabled", ignoreCase = true) == true -> true
                wifiLine?.contains("disabled", ignoreCase = true) == true -> false
                else -> null
            }

            val dataResult =
                runLoggedBlocking(deviceId, listOf("shell", "dumpsys", "telephony.registry"))
            val dataLine =
                dataResult.stdout.lines().firstOrNull { it.contains("mDataConnectionState") }
            _dataEnabled.value = when {
                dataLine == null -> null
                dataLine.contains("CONNECTED", ignoreCase = true) -> true
                Regex("mDataConnectionState=(\\d+)").find(dataLine)?.groupValues?.get(1) == "2" -> true
                else -> false
            }
        }
    }

    fun startScreenRecord(deviceId: String?, devicePath: String) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        if (devicePath.isBlank()) {
            setActionError("Invalid recording path")
            return
        }
        logAdbCommand(deviceId, formatCommand(deviceId, "shell screenrecord $devicePath"))
        scope.launch {
            // runDetached, not runCommand: screenrecord runs until stopScreenRecord signals it,
            // so it must not be awaited — and must not be subject to the command timeout, which
            // would cut the recording short.
            val error = repository.runDetached(deviceId, listOf("shell", "screenrecord", devicePath))
            if (error != null) setActionError(error) else setActionMessage("Screen recording started")
        }
    }

    fun stopScreenRecord(deviceId: String?, devicePath: String?, localPath: String?) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        if (devicePath.isNullOrBlank() || localPath.isNullOrBlank()) {
            setActionError("Recording paths missing")
            return
        }
        scope.launch {
            val pidResult = runLoggedBlocking(deviceId, listOf("shell", "pidof", "screenrecord"))
            val pid = pidResult.stdout.trim().split(Regex("\\s+")).firstOrNull { it.isNotBlank() }
            if (pid.isNullOrBlank()) {
                setActionError("Unable to stop screen recording (pid not found)")
                return@launch
            }
            runLoggedBlocking(deviceId, listOf("shell", "kill", "-2", pid))
            waitForScreenrecordExit(deviceId)
            delay(500)
            runLoggedBlocking(deviceId, listOf("pull", devicePath, localPath))
            runLoggedBlocking(deviceId, listOf("shell", "rm", devicePath))
            setActionMessage("Screen recording saved")
        }
    }

    fun takeScreenshot(deviceId: String?, localPath: String) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        if (localPath.isBlank()) {
            setActionError("Screenshot paths missing")
            return
        }
        val devicePath = "/sdcard/${File(localPath).name}"
        scope.launch {
            runLoggedBlocking(deviceId, listOf("shell", "screencap", "-p", devicePath))
            runLoggedBlocking(deviceId, listOf("pull", devicePath, localPath))
            runLoggedBlocking(deviceId, listOf("shell", "rm", devicePath))
            setActionMessage("Screenshot saved")
        }
    }

    fun takeBugreport(deviceId: String?, devicePath: String, localPath: String) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        if (devicePath.isBlank() || localPath.isBlank()) {
            setActionError("Bugreport paths missing")
            return
        }
        scope.launch {
            runLoggedBlocking(deviceId, listOf("bugreport", devicePath))
            runLoggedBlocking(deviceId, listOf("pull", devicePath, localPath))
            runLoggedBlocking(deviceId, listOf("shell", "rm", devicePath))
            setActionMessage("Bugreport saved")
        }
    }

    fun openDeepLink(deviceId: String?, url: String) {
        if (deviceId.isNullOrBlank()) {
            setActionError("Select a device first")
            return
        }
        if (url.isBlank()) {
            setActionError("Enter a deep link URL")
            return
        }
        scope.launch {
            runLoggedBlocking(
                deviceId,
                listOf("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", url),
            )
            deepLinkHistoryStore.add(url)
            _deepLinkHistory.value = deepLinkHistoryStore.load()
            setActionMessage("Deep link opened")
        }
    }

    fun removeDeepLinkEntry(url: String) {
        scope.launch {
            deepLinkHistoryStore.remove(url)
            _deepLinkHistory.value = deepLinkHistoryStore.load()
        }
    }

    fun clearDeepLinkHistory() {
        scope.launch {
            deepLinkHistoryStore.clear()
            _deepLinkHistory.value = emptyList()
        }
    }

    private fun Device.toUi(): DeviceUi = DeviceUi(
        id = id,
        state = state,
        model = model,
        platform = platform,
        capabilities = capabilities,
        usbmuxDeviceId = usbmuxDeviceId,
    )

    private fun logAdbCommand(deviceId: String?, command: String) {
        val entry = AdbCommandLogEntry(
            timestamp = System.currentTimeMillis(),
            deviceId = deviceId,
            command = command,
        )
        _adbCommandHistory.value = (_adbCommandHistory.value + entry).takeLast(100)
    }

    private fun formatCommand(deviceId: String?, rawCommand: String): String {
        val trimmed = rawCommand.trim()
        if (trimmed.startsWith("adb ")) {
            return trimmed
        }
        return buildString {
            append("adb ")
            if (!deviceId.isNullOrBlank()) {
                append("-s ")
                append(deviceId)
                append(" ")
            }
            append(trimmed)
        }
    }

    private suspend fun runLoggedBlocking(deviceId: String?, args: List<String>): CommandResult {
        val commandText = formatCommand(deviceId, args.joinToString(" "))
        logAdbCommand(deviceId, commandText)
        return repository.runCommandBlocking(deviceId, args)
    }

    private suspend fun waitForScreenrecordExit(deviceId: String?) {
        if (deviceId.isNullOrBlank()) return
        repeat(10) {
            val pidResult = runLoggedBlocking(deviceId, listOf("shell", "pidof", "screenrecord"))
            if (pidResult.stdout.isBlank()) return
            delay(200)
        }
    }

    /**
     * Cancels this view model's scope.
     *
     * Required for per-window teardown: DesktopAppComposition.close() used to cancel
     * only DevToolsViewModel, so every other scope (and its collectors) leaked for the
     * life of the process each time a device window was closed.
     */
    fun close() {
        scope.cancel()
    }
}
