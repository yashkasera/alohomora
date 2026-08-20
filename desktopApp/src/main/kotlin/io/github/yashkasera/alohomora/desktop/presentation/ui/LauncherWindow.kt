package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState
import io.github.yashkasera.alohomora.desktop.app.DesktopAppComposition
import io.github.yashkasera.alohomora.desktop.app.DesktopBuildConfig
import io.github.yashkasera.alohomora.desktop.app.MacTitleBarHeight
import io.github.yashkasera.alohomora.desktop.app.applyMacTitleBar
import io.github.yashkasera.alohomora.desktop.app.isMacOs
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.UpdateBanner
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import java.awt.Dimension
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
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

@Composable
fun LauncherWindow(
    sharedComposition: DesktopAppComposition,
    isDarkState: MutableState<Boolean>,
    themeId: String,
    updateInfo: UpdateInfo?,
    updateDismissed: Boolean,
    onDismissUpdate: () -> Unit,
    onShowSettings: () -> Unit,
    onShowAbout: () -> Unit,
    onOpenDeviceWindow: (deviceId: String, host: String, hostPort: Int, devicePort: Int, composition: DesktopAppComposition) -> Unit,
    onClose: () -> Unit,
    onExit: () -> Unit,
) {
    val state = rememberWindowState(size = DpSize(900.dp, 560.dp))
    Window(
        title = "Alohomora",
        state = state,
        onCloseRequest = onClose,
        resizable = false,
    ) {
        AppTheme(isDarkState = isDarkState, themeId = themeId) {
            MenuBar {
                Menu("File") {
                    Item(
                        "Preferences",
                        shortcut = KeyShortcut(Key.Comma, meta = isMacOs, ctrl = !isMacOs),
                        onClick = onShowSettings,
                    )
                    Separator()
                    Item("Exit", onClick = onExit)
                }
                Menu("Help") {
                    Item("About Alohomora", onClick = onShowAbout)
                }
            }
            window.minimumSize = Dimension(900, 560)
            applyMacTitleBar(window)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = if (isMacOs) MacTitleBarHeight else 0.dp),
            ) {
                Column {
                    if (updateInfo != null && !updateDismissed) {
                        UpdateBanner(
                            updateInfo = updateInfo,
                            onDismiss = onDismissUpdate,
                        )
                    }

                    LauncherContent(
                        sharedDevicesComposition = sharedComposition,
                        onOpenDeviceWindow = onOpenDeviceWindow,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LauncherContent(
    sharedDevicesComposition: DesktopAppComposition,
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
            delay(3000.milliseconds)
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
                    onOpenDeviceWindow(
                        session.deviceId,
                        session.host,
                        session.hostPort,
                        session.devicePort,
                        session.composition,
                    )
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
                .padding(MaterialTheme.dimens.margin.xxl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AlohomoraFull,
                    contentDescription = "Alohomora",
                    modifier = Modifier.width(256.dp),
                )
                Spacer(modifier = Modifier.weight(1f))
                AlohomoraOutlinedButton(
                    text = "Refresh",
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                            imageVector = Icons.RefreshCw, contentDescription = null,
                        )
                    },
                    onClick = devicesViewModel::refreshDevices,
                    size = AlohomoraButtonSize.SMALL,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.margin.md))

            if (pendingSession != null) {
                when (val pending = pendingConnectionState) {
                    is DevToolsConnection.AwaitingAuth if pending.otpRequired -> {
                        Text(
                            "Authentication Required",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Text(
                            "Enter the 4-digit code shown on your device.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        AlohomoraTextField(
                            value = otpInput,
                            onValueChange = {
                                if (it.length <= 4 && it.all(Char::isDigit)) otpInput = it
                            },
                            placeholder = "0000",
                            singleLine = true,
                            modifier = Modifier.width(160.dp),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AlohomoraFilledButton(
                                text = "Confirm",
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.submitOtp(
                                        otpInput,
                                    )
                                    otpInput = ""
                                },
                                enabled = otpInput.length == 4,
                                uppercase = false,
                            )
                            AlohomoraOutlinedButton(
                                text = "Cancel",
                                size = AlohomoraButtonSize.SMALL,
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.disconnect()
                                    pendingSession = null
                                    otpInput = ""
                                },
                            )
                        }
                    }

                    else -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(
                                space = MaterialTheme.dimens.margin.md,
                                alignment = Alignment.CenterVertically,
                            ),
                        ) {
                            CircularWavyProgressIndicator()
                            Text(
                                "Connecting…",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            (pendingConnectionState as? DevToolsConnection.Connecting)?.let { conn ->
                                Text(
                                    "${conn.host}:${conn.port}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                            AlohomoraOutlinedButton(
                                text = "Cancel",
                                size = AlohomoraButtonSize.SMALL,
                                onClick = {
                                    pendingSession?.composition?.devToolsViewModel?.disconnect()
                                    pendingSession = null
                                },
                            )
                        }
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
                                    .clip(MaterialTheme.shapes.small)
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
                    AlohomoraFilledButton(
                        text = "Connect & Open Window",
                        onClick = {
                            if (selectedDevice == null) {
                                actionError = "Select an online device first"
                                return@AlohomoraFilledButton
                            }
                            val numericHostPort = hostPort.toIntOrNull() ?: DEFAULT_PORT.toInt()
                            val numericDevicePort = devicePort.toIntOrNull() ?: DEFAULT_PORT.toInt()
                            val isLocalHost = host == "127.0.0.1" || host == "localhost"

                            val composition =
                                DesktopAppComposition(sharedDevicesViewModel = devicesViewModel)

                            fun openSession(target1: DeviceUi, tunnel: DevToolsTarget) {
                                composition.devToolsViewModel.switchDevice(tunnel, target1.id)
                                pendingSession = PendingSession(
                                    target1.id,
                                    tunnel.displayHost,
                                    numericHostPort,
                                    numericDevicePort,
                                    composition,
                                )
                                actionError = null
                            }

                            scope.launch {
                                when (selectedDevice.platform) {
                                    DevicePlatform.IOS -> {
                                        val usbmuxId = selectedDevice.usbmuxDeviceId
                                        if (usbmuxId == null) {
                                            actionError =
                                                "Device is not reachable over USB. Reconnect the cable and trust this Mac."
                                        } else {
                                            openSession(
                                                selectedDevice,
                                                DevToolsTarget.Usbmux(usbmuxId, numericDevicePort),
                                            )
                                        }
                                    }

                                    DevicePlatform.IOS_SIMULATOR ->
                                        openSession(
                                            selectedDevice,
                                            DevToolsTarget.Tcp("127.0.0.1", numericDevicePort),
                                        )

                                    DevicePlatform.ANDROID -> {
                                        if (!isLocalHost) {
                                            val connectError = suspendConnectOverTcp(
                                                composition.devicesViewModel,
                                                selectedDevice.id, host, numericDevicePort,
                                            )
                                            if (connectError != null) {
                                                actionError = connectError
                                                return@launch
                                            }
                                        }
                                        val selectError = suspendSelectDevice(
                                            composition.devicesViewModel,
                                            selectedDevice.id, numericHostPort, numericDevicePort,
                                        )
                                        if (selectError != null) {
                                            actionError = selectError
                                        } else {
                                            openSession(
                                                selectedDevice,
                                                DevToolsTarget.Tcp(host, numericHostPort),
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        enabled = selectedDevice != null,
                    )
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
