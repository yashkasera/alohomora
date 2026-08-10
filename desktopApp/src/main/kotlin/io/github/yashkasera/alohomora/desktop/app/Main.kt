package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.rememberWindowState
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.DevToolsDesktopApp
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.Alohomora
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateChecker
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AboutDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.UpdateBanner
import java.awt.Dimension
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

private const val DEFAULT_HOST = "127.0.0.1"
private const val DEFAULT_PORT = "53999"

private data class PendingSession(
    val deviceId: String,
    val host: String,
    val hostPort: Int,
    val devicePort: Int,
    val composition: DesktopAppComposition,
)

data class DeviceWindowSession(
    val id: String = UUID.randomUUID().toString(),
    val deviceId: String,
    val host: String,
    val hostPort: Int,
    val devicePort: Int,
    val composition: DesktopAppComposition,
)

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val initialIsDark = DesktopThemePrefs.load()
    System.setProperty("apple.awt.application.name", "Alohomora")
    System.setProperty(
        "apple.awt.application.appearance",
        if (initialIsDark) "NSAppearanceNameDarkAqua" else "NSAppearanceNameAqua",
    )
    application {
        val sharedComposition = remember { DesktopAppComposition() }
        val sessions = remember { mutableStateListOf<DeviceWindowSession>() }
        var launcherVisible by remember { mutableStateOf(true) }
        val sharedIsDark = remember { mutableStateOf(initialIsDark) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var updateDismissed by remember { mutableStateOf(false) }
        var showAbout by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            snapshotFlow { sharedIsDark.value }
                .drop(1)
                .collect { DesktopThemePrefs.save(it) }
        }

        LaunchedEffect(Unit) {
            val info = UpdateChecker.check(DesktopBuildConfig.version)
            if (info != null && info.isUpdateAvailable) {
                updateInfo = info
            }
        }

        if (showAbout) {
            AboutDialog(
                isDark = sharedIsDark.value,
                updateInfo = updateInfo,
                onDismiss = { showAbout = false },
            )
        }

        if (launcherVisible) {
            val state = rememberDialogState()
            DialogWindow(
                title = "Alohomora",
                state = state,
                onCloseRequest = {
                    launcherVisible = false
                    if (sessions.isEmpty()) exitApplication()
                },
                resizable = false
            ) {
                AppTheme(isDarkState = sharedIsDark) {
                    window.minimumSize = Dimension(900, 560)

                    Column {
                        val pending = updateInfo
                        if (pending != null && !updateDismissed) {
                            UpdateBanner(
                                updateInfo = pending,
                                onDismiss = { updateDismissed = true },
                            )
                        }

                        LauncherScreen(
                            sharedDevicesComposition = sharedComposition,
                            onCloseLauncher = {
                                launcherVisible = false
                                if (sessions.isEmpty()) exitApplication()
                            },
                            onOpenDeviceWindow = { deviceId, host, hostPort, devicePort, composition ->
                                val duplicate = sessions.any { it.deviceId == deviceId }
                                if (!duplicate) {
                                    sessions += DeviceWindowSession(
                                        deviceId = deviceId,
                                        host = host,
                                        hostPort = hostPort,
                                        devicePort = devicePort,
                                        composition = composition,
                                    )
                                }
                                launcherVisible = false
                            },
                        )
                    }
                }
            }
        }

        sessions.toList().forEach { session ->
            key(session.id) {
                val state = rememberWindowState(
                    placement = WindowPlacement.Maximized,
                    size = DpSize(1080.dp, 600.dp)
                )
                var showHelp by remember { mutableStateOf(false) }
                var showCommandPalette by remember { mutableStateOf(false) }

                val closeWindow = {
                    val devicesVm = session.composition.devicesViewModel
                    val devToolsVm = session.composition.devToolsViewModel
                    devicesVm.disconnectHost(session.host, session.hostPort)
                    devicesVm.deactivateDevice(session.deviceId, session.hostPort)
                    devToolsVm.disconnect()
                    session.composition.close()
                    sessions.removeAll { it.id == session.id }
                    if (sessions.isEmpty()) launcherVisible = true
                }

                var deviceWasOnline by remember { mutableStateOf(true) }
                val devicesForReforward by session.composition.devicesViewModel.devices.collectAsState()
                val deviceIsOnline = devicesForReforward.any { it.id == session.deviceId && it.state == DeviceState.DEVICE }
                LaunchedEffect(deviceIsOnline) {
                    if (deviceIsOnline && !deviceWasOnline) {
                        val device = devicesForReforward.firstOrNull { it.id == session.deviceId }
                        if (device?.platform == DevicePlatform.ANDROID) {
                            session.composition.devicesViewModel.selectDevice(
                                session.deviceId, session.hostPort, session.devicePort,
                            )
                        }
                    }
                    deviceWasOnline = deviceIsOnline
                }

                Window(
                    title = "Alohomora - ${session.deviceId}",
                    state = state,
                    onCloseRequest = {
                        session.composition.devToolsViewModel.disconnect()
                        session.composition.close()
                        sessions.removeAll { it.id == session.id }
                        if (sessions.isEmpty()) launcherVisible = true
                    },
                ) {
                    AppTheme(isDarkState = sharedIsDark) {
                        MenuBar {
                            Menu("File") {
                                Item(
                                    "New Window",
                                    shortcut = KeyShortcut(Key.N, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = { launcherVisible = true },
                                )
                                Item(
                                    "Close Window",
                                    shortcut = KeyShortcut(Key.W, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = closeWindow,
                                )
                                Item("Exit", onClick = ::exitApplication)
                            }
                            Menu("View") {
                                Item(
                                    "Toggle Theme",
                                    shortcut = KeyShortcut(Key.T, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = { sharedIsDark.value = !sharedIsDark.value },
                                )
                            }
                            Menu("Device") {
                                Item(
                                    "Take Screenshot",
                                    shortcut = KeyShortcut(Key.S, shift = true, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = {
                                        val timestamp = System.currentTimeMillis()
                                        val defaultName = "alohomora_screenshot_${timestamp}.png"
                                        val localPath = io.github.yashkasera.alohomora.desktop.util.pickSavePath(
                                            defaultName, "Save Screenshot", ".png",
                                        ) ?: return@Item
                                        session.composition.devicesViewModel.takeScreenshot(session.deviceId, localPath)
                                    },
                                )
                            }
                            Menu("Help") {
                                Item(
                                    "Command Palette",
                                    shortcut = KeyShortcut(Key.K, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = { showCommandPalette = true },
                                )
                                Item(
                                    "Keyboard Shortcuts",
                                    shortcut = KeyShortcut(Key.Slash, meta = isMacOs, ctrl = !isMacOs),
                                    onClick = { showHelp = true },
                                )
                                Item(
                                    "About Alohomora",
                                    onClick = { showAbout = true },
                                )
                            }
                        }
                        window.minimumSize = Dimension(1080, 600)

                        Column {
                            val pending = updateInfo
                            if (pending != null && !updateDismissed) {
                                UpdateBanner(
                                    updateInfo = pending,
                                    onDismiss = { updateDismissed = true },
                                )
                            }

                            DevToolsDesktopApp(
                                devToolsViewModel = session.composition.devToolsViewModel,
                                devicesViewModel = session.composition.devicesViewModel,
                                logcatViewModel = session.composition.logcatViewModel,
                                databaseViewModel = session.composition.databaseViewModel,
                                cacheViewModel = session.composition.cacheViewModel,
                                tracesViewModel = session.composition.tracesViewModel,
                                eventsViewModel = session.composition.eventsViewModel,
                                trafficViewModel = session.composition.trafficViewModel,
                                networkRulesViewModel = session.composition.networkRulesViewModel,
                                initialDeviceId = session.deviceId,
                                showHelp = showHelp,
                                onShowHelp = { showHelp = true },
                                onDismissHelp = { showHelp = false },
                                showCommandPalette = showCommandPalette,
                                onDismissCommandPalette = { showCommandPalette = false },
                                onToggleTheme = { sharedIsDark.value = !sharedIsDark.value },
                                isDark = sharedIsDark.value,
                                onDisconnectWindow = closeWindow,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LauncherScreen(
    sharedDevicesComposition: DesktopAppComposition,
    onCloseLauncher: () -> Unit,
    onOpenDeviceWindow: (deviceId: String, host: String, hostPort: Int, devicePort: Int, composition: DesktopAppComposition) -> Unit,
) {
    val devicesViewModel = sharedDevicesComposition.devicesViewModel
    val devices by devicesViewModel.devices.collectAsState()

    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var host by remember { mutableStateOf(DEFAULT_HOST) }
    var hostPort by remember { mutableStateOf(DEFAULT_PORT) }
    var devicePort by remember { mutableStateOf(DEFAULT_PORT) }
    var actionError by remember { mutableStateOf<String?>(null) }

    var pendingSession by remember { mutableStateOf<PendingSession?>(null) }
    var pendingConnectionState by remember { mutableStateOf<DevToolsConnection>(DevToolsConnection.Disconnected) }
    var otpInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    val onlineDevices = devices.filter { it.state == DeviceState.DEVICE }
    val selectedDevice = onlineDevices.firstOrNull { it.id == selectedDeviceId }

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000)
        }
    }

    LaunchedEffect(onlineDevices) {
        if (selectedDeviceId == null || onlineDevices.none { it.id == selectedDeviceId }) {
            selectedDeviceId = onlineDevices.firstOrNull()?.id
        }
    }

    LaunchedEffect(pendingSession) {
        val session = pendingSession ?: return@LaunchedEffect
        session.composition.devToolsViewModel.uiState.collect { uiState ->
            pendingConnectionState = uiState.connection
            when (val conn = uiState.connection) {
                is DevToolsConnection.Connected -> {
                    onOpenDeviceWindow(session.deviceId, session.host, session.hostPort, session.devicePort, session.composition)
                    pendingSession = null
                    otpInput = ""
                }
                is DevToolsConnection.Failed -> {
                    actionError = conn.reason
                    pendingSession = null
                    otpInput = ""
                }
                else -> {}
            }
        }
    }

    Surface {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Alohomora,
                    contentDescription = null,
                    modifier = Modifier.size(
                        MaterialTheme.dimens.icon.standard
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                AlohomoraOutlinedButton(
                    text = "Refresh",
                    leadingIcon = { Icon(
                        modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        imageVector = Icons.RefreshCw, contentDescription = null) },
                    onClick = devicesViewModel::refreshDevices,
                    size = AlohomoraButtonSize.SMALL
                )
            }

            if (pendingSession != null) {
                when (val pending = pendingConnectionState) {
                    is DevToolsConnection.AwaitingAuth if pending.otpRequired -> {
                        Text(
                            "Authentication Required",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Enter the 4-digit code shown on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        AlohomoraTextField(
                            value = otpInput,
                            onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) otpInput = it },
                            placeholder = "0000",
                            singleLine = true,
                            modifier = Modifier.width(160.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.submitOtp(otpInput)
                                    otpInput = ""
                                },
                                enabled = otpInput.length == 4,
                            ) { Text("Confirm") }
                            OutlinedButton(onClick = {
                                pendingSession?.composition?.devToolsViewModel?.disconnect()
                                pendingSession = null
                                otpInput = ""
                            }) { Text("Cancel") }
                        }
                    }
                    else -> {
                        Text(
                            "Connecting…",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        (pendingConnectionState as? DevToolsConnection.Connecting)?.let { conn ->
                            Text(
                                "${conn.host}:${conn.port}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        OutlinedButton(onClick = {
                            pendingSession?.composition?.devToolsViewModel?.disconnect()
                            pendingSession = null
                        }) { Text("Cancel") }
                    }
                }
                if (!actionError.isNullOrBlank()) {
                    Text(actionError!!, color = MaterialTheme.colorScheme.error)
                }
            } else {
                Text(
                    "Select a device, connect, and open a dedicated window.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )

                if (onlineDevices.isEmpty()) {
                    Text(
                        "No online devices found. Connect a device and refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        onlineDevices.forEach { device ->
                            val selected = device.id == selectedDeviceId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                    )
                                    .clickable { selectedDeviceId = device.id }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                            ) {
                                Text(device.model ?: device.id)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(device.id, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlohomoraTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = "Host",
                        singleLine = true,
                        modifier = Modifier.width(220.dp),
                    )
                    AlohomoraTextField(
                        value = hostPort,
                        onValueChange = { hostPort = it.filter(Char::isDigit) },
                        label = "Host Port",
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                    AlohomoraTextField(
                        value = devicePort,
                        onValueChange = { devicePort = it.filter(Char::isDigit) },
                        label = "Device Port",
                        singleLine = true,
                        modifier = Modifier.width(140.dp),
                    )
                }

                if (!actionError.isNullOrBlank()) {
                    Text(actionError!!, color = MaterialTheme.colorScheme.error)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val target = selectedDevice
                            if (target == null) {
                                actionError = "Select an online device first"
                                return@Button
                            }
                            val numericHostPort = hostPort.toIntOrNull() ?: DEFAULT_PORT.toInt()
                            val numericDevicePort = devicePort.toIntOrNull() ?: DEFAULT_PORT.toInt()
                            val isLocalHost = host == "127.0.0.1" || host == "localhost"

                            val composition = DesktopAppComposition(sharedDevicesViewModel = devicesViewModel)

                            fun openSession(target1: DeviceUi, tunnel: DevToolsTarget) {
                                composition.devToolsViewModel.switchDevice(tunnel, target1.id)
                                pendingSession = PendingSession(
                                    target1.id, tunnel.displayHost, numericHostPort, numericDevicePort, composition,
                                )
                                actionError = null
                            }

                            scope.launch {
                                when (target.platform) {
                                    // Physical iOS: no adb, and no host-side port to reserve.
                                    // usbmuxd tunnels straight to the device port over USB.
                                    DevicePlatform.IOS -> {
                                        val usbmuxId = target.usbmuxDeviceId
                                        if (usbmuxId == null) {
                                            actionError = "Device is not reachable over USB. Reconnect the cable and trust this Mac."
                                        } else {
                                            openSession(target, DevToolsTarget.Usbmux(usbmuxId, numericDevicePort))
                                        }
                                    }

                                    // Simulator: nothing to tunnel. It runs on the host's network
                                    // stack, so the device's 127.0.0.1 is the host's 127.0.0.1.
                                    DevicePlatform.IOS_SIMULATOR ->
                                        openSession(target, DevToolsTarget.Tcp("127.0.0.1", numericDevicePort))

                                    DevicePlatform.ANDROID -> {
                                        // Wi-Fi adb needs `adb connect` before a forward exists.
                                        if (!isLocalHost) {
                                            val connectError = suspendConnectOverTcp(
                                                composition.devicesViewModel, target.id, host, numericDevicePort,
                                            )
                                            if (connectError != null) {
                                                actionError = connectError
                                                return@launch
                                            }
                                        }
                                        val selectError = suspendSelectDevice(
                                            composition.devicesViewModel, target.id, numericHostPort, numericDevicePort,
                                        )
                                        if (selectError != null) {
                                            actionError = selectError
                                        } else {
                                            openSession(target, DevToolsTarget.Tcp(host, numericHostPort))
                                        }
                                    }
                                }
                            }
                        },
                        enabled = selectedDevice != null,
                    ) {
                        Text("Connect & Open Window")
                    }
                    OutlinedButton(onClick = onCloseLauncher) {
                        Text("Close")
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = "v${DesktopBuildConfig.version}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

/**
 * Callback-to-suspend adapters for the device activation calls.
 *
 * The launcher previously nested these four callbacks deep inside an onClick, which made the
 * ordering (adb connect -> forward -> DevTools switch) impossible to follow and impossible to
 * short-circuit on error.
 */
private suspend fun suspendConnectOverTcp(
    viewModel: DevicesViewModel,
    deviceId: String,
    host: String,
    port: Int,
): String? = suspendCancellableCoroutine { continuation ->
    viewModel.connectOverTcp(deviceId, host, port) { error -> continuation.resume(error) }
}

private suspend fun suspendSelectDevice(
    viewModel: DevicesViewModel,
    deviceId: String,
    hostPort: Int,
    devicePort: Int,
): String? = suspendCancellableCoroutine { continuation ->
    viewModel.selectDevice(deviceId, hostPort, devicePort) { error -> continuation.resume(error) }
}
