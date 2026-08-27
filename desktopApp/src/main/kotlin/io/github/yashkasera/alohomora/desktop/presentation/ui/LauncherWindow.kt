package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import io.github.yashkasera.alohomora.desktop.data.adb.WirelessQr
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.domain.model.WirelessDiscovery
import io.github.yashkasera.alohomora.desktop.domain.model.WirelessEndpoint
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.QrCode
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.UpdateBanner
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import io.github.yashkasera.alohomora.ui.icons.Android
import io.github.yashkasera.alohomora.ui.icons.Apple
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Link
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

private enum class LauncherState { DEVICE_SELECT, CONNECTING, AUTH_OTP }

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
    appOverlays: @Composable () -> Unit,
    onOpenDeviceWindow: (deviceId: String, host: String, hostPort: Int, devicePort: Int, composition: DesktopAppComposition) -> Unit,
    onClose: () -> Unit,
    onExit: () -> Unit,
) {
    val state = rememberWindowState(size = DpSize(900.dp, 720.dp))
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(top = if (isMacOs) MacTitleBarHeight else 0.dp),
            ) {
                LauncherContent(
                    modifier = Modifier.weight(1f),
                    sharedDevicesComposition = sharedComposition,
                    onOpenDeviceWindow = onOpenDeviceWindow,
                )

                if (updateInfo != null && !updateDismissed) {
                    UpdateBanner(
                        updateInfo = updateInfo,
                        onDismiss = onDismissUpdate,
                        modifier = Modifier
                    )
                }
            }
            appOverlays()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LauncherContent(
    modifier: Modifier = Modifier,
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
    var showPairingDialog by remember { mutableStateOf(false) }

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

    val launcherState = when {
        pendingSession == null -> LauncherState.DEVICE_SELECT
        pendingConnectionState is DevToolsConnection.AwaitingAuth &&
            (pendingConnectionState as DevToolsConnection.AwaitingAuth).otpRequired ->
            LauncherState.AUTH_OTP

        else -> LauncherState.CONNECTING
    }

    Surface(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(MaterialTheme.dimens.margin.xxl),
        ) {
            AnimatedContent(
                targetState = launcherState,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(spring()) + slideInVertically(spring(dampingRatio = 0.8f)) { it / 12 })
                        .togetherWith(fadeOut(spring()) + slideOutVertically(spring()) { -it / 12 })
                },
                label = "launcher-state",
            ) { state ->
                when (state) {
                    LauncherState.DEVICE_SELECT -> DeviceSelectContent(
                        onlineDevices = onlineDevices,
                        selectedDeviceId = selectedDeviceId,
                        selectedDevice = selectedDevice,
                        host = host,
                        hostPort = hostPort,
                        devicePort = devicePort,
                        actionError = actionError,
                        onDeviceSelect = { selectedDeviceId = it },
                        onHostChange = { host = it },
                        onHostPortChange = { hostPort = it.filter(Char::isDigit) },
                        onDevicePortChange = { devicePort = it.filter(Char::isDigit) },
                        onRefresh = devicesViewModel::refreshDevices,
                        onPairWifi = { showPairingDialog = true },
                        onConnect = {
                            if (selectedDevice == null) {
                                actionError = "Select an online device first"
                                return@DeviceSelectContent
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
                    )

                    LauncherState.CONNECTING -> ConnectingContent(
                        connectionState = pendingConnectionState,
                        onCancel = {
                            pendingSession?.composition?.devToolsViewModel?.disconnect()
                            pendingSession = null
                        },
                    )

                    LauncherState.AUTH_OTP -> AuthOtpContent(
                        otpInput = otpInput,
                        onOtpChange = {
                            if (it.length <= 4 && it.all(Char::isDigit)) otpInput = it
                        },
                        onConfirm = {
                            pendingSession?.composition?.devToolsViewModel?.submitOtp(otpInput)
                            otpInput = ""
                        },
                        onCancel = {
                            pendingSession?.composition?.devToolsViewModel?.disconnect()
                            pendingSession = null
                            otpInput = ""
                        },
                    )
                }
            }
        }
    }

    if (showPairingDialog) {
        WirelessPairingDialog(
            viewModel = devicesViewModel,
            onConnected = { connectHost, connectPort ->
                // The serial adb assigns a wireless device is exactly host:port. Selecting it now
                // means the poll that adds it lands on the right device, ready for Connect.
                selectedDeviceId = "$connectHost:$connectPort"
                actionError = null
                showPairingDialog = false
                devicesViewModel.refreshDevices()
            },
            onDismiss = { showPairingDialog = false },
        )
    }
}

// region Device Selection

@Composable
private fun DeviceSelectContent(
    onlineDevices: List<DeviceUi>,
    selectedDeviceId: String?,
    selectedDevice: DeviceUi?,
    host: String,
    hostPort: String,
    devicePort: String,
    actionError: String?,
    onDeviceSelect: (String) -> Unit,
    onHostChange: (String) -> Unit,
    onHostPortChange: (String) -> Unit,
    onDevicePortChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onConnect: () -> Unit,
    onPairWifi: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
    ) {
        // Left column — branding + connection config
        Column(
            modifier = Modifier.weight(0.4f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(MaterialTheme.dimens.margin.xs)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .padding(
                                start = MaterialTheme.dimens.margin.xxl,
                                end = MaterialTheme.dimens.margin.xxl,
                                top = MaterialTheme.dimens.margin.xxxl,
                                bottom = MaterialTheme.dimens.margin.xxl,
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = Icons.AlohomoraFull,
                            contentDescription = "Alohomora",
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
                        Text(
                            "Developer Console",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Text(
                                "v${DesktopBuildConfig.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(
                                    horizontal = MaterialTheme.dimens.margin.sm,
                                    vertical = MaterialTheme.dimens.margin.xs,
                                ),
                            )
                        }
                    }
                }
            }

            AlohomoraOutlinedCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                ) {
                    Text(
                        "Connection",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    AlohomoraTextField(
                        value = host,
                        onValueChange = onHostChange,
                        label = "Host",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
                        AlohomoraTextField(
                            value = hostPort,
                            onValueChange = onHostPortChange,
                            label = "Host Port",
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        AlohomoraTextField(
                            value = devicePort,
                            onValueChange = onDevicePortChange,
                            label = "Device Port",
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        "No cable? Pair over Wi-Fi using Android 11+ Wireless debugging, then " +
                            "connect the paired device below.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AlohomoraOutlinedButton(
                        text = "Pair over Wi-Fi",
                        onClick = onPairWifi,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                Icons.Link,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            )
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = !actionError.isNullOrBlank(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.CircleAlert,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                        Text(
                            actionError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
        }

        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Right column — device list
        Column(
            modifier = Modifier.weight(0.6f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Devices",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (onlineDevices.isNotEmpty()) {
                    Spacer(Modifier.width(MaterialTheme.dimens.margin.sm))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Text(
                            "${onlineDevices.size}",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(
                                horizontal = MaterialTheme.dimens.margin.sm,
                                vertical = MaterialTheme.dimens.margin.xs,
                            ),
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                AlohomoraOutlinedButton(
                    text = "Refresh",
                    leadingIcon = {
                        Icon(
                            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                            imageVector = Icons.RefreshCw,
                            contentDescription = null,
                        )
                    },
                    onClick = onRefresh,
                    size = AlohomoraButtonSize.SMALL,
                )
            }

            if (onlineDevices.isEmpty()) {
                EmptyState(
                    icon = Icons.HardDrive,
                    title = "No devices found",
                    subtitle = "Connect a device via USB and refresh",
                    modifier = Modifier.weight(1f),
                    action = {
                        AlohomoraOutlinedButton(
                            text = "Refresh Devices",
                            onClick = onRefresh,
                        )
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    itemsIndexed(onlineDevices, key = { _, d -> d.id }) { _, device ->
                        DeviceListCard(
                            device = device,
                            selected = device.id == selectedDeviceId,
                            onClick = { onDeviceSelect(device.id) },
                        )
                    }
                }

                AlohomoraFilledButton(
                    text = "Connect & Open Window",
                    onClick = onConnect,
                    enabled = selectedDevice != null,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            Icons.Link,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DeviceListCard(
    device: DeviceUi,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else AlohomoraCardDefaults.colors().containerColor,
        animationSpec = spring(),
        label = "device-card-color",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) MaterialTheme.dimens.stroke.medium else 0.dp,
        animationSpec = spring(),
        label = "device-card-border",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = spring(),
        label = "device-card-border-color",
    )

    AlohomoraCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, AlohomoraCardDefaults.shape),
        colors = AlohomoraCardDefaults.colors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(MaterialTheme.dimens.icon.xl),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = when (device.platform) {
                            DevicePlatform.IOS, DevicePlatform.IOS_SIMULATOR -> Icons.Apple
                            else -> Icons.Android
                        },
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.model ?: device.id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        when (device.platform) {
                            DevicePlatform.IOS -> "iOS"
                            DevicePlatform.IOS_SIMULATOR -> "iOS Sim"
                            DevicePlatform.ANDROID -> "Android"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        device.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ConnectionStatusDot(
                state = ConnectionDotState.Connected,
                size = MaterialTheme.dimens.icon.xs,
            )
        }
    }
}

// endregion

// region Connecting State

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ConnectingContent(
    connectionState: DevToolsConnection,
    onCancel: () -> Unit,
) {
    val stepIndex = when (connectionState) {
        is DevToolsConnection.Connecting -> 0
        is DevToolsConnection.AwaitingAuth -> 1
        is DevToolsConnection.Connected -> 2
        else -> 0
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AlohomoraCard(
            modifier = Modifier.fillMaxWidth(0.6f),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xxxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
            ) {
                ConnectionStepper(activeStep = stepIndex)

                CircularWavyProgressIndicator()

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Connecting…",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    (connectionState as? DevToolsConnection.Connecting)?.let { conn ->
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
                        Text(
                            "${conn.host}:${conn.port}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }

                AlohomoraOutlinedButton(
                    text = "Cancel",
                    size = AlohomoraButtonSize.SMALL,
                    onClick = onCancel,
                )
            }
        }
    }
}

@Composable
private fun ConnectionStepper(activeStep: Int) {
    val steps = listOf("Port Forward", "Handshake", "Connected")
    val dotSize = MaterialTheme.dimens.icon.lg
    val lineThickness = MaterialTheme.dimens.stroke.medium
    val lineTopOffset = (dotSize - lineThickness) / 2

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
    ) {
        steps.forEachIndexed { index, label ->
            if (index > 0) {
                val lineColor by animateColorAsState(
                    targetValue = if (index <= activeStep) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant,
                    animationSpec = spring(),
                    label = "stepper-line-$index",
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f).padding(top = lineTopOffset),
                    thickness = lineThickness,
                    color = lineColor,
                )
            }

            StepIndicator(
                label = label,
                state = when {
                    index < activeStep -> StepState.COMPLETED
                    index == activeStep -> StepState.ACTIVE
                    else -> StepState.UPCOMING
                },
            )
        }
    }
}

private enum class StepState { UPCOMING, ACTIVE, COMPLETED }

@Composable
private fun StepIndicator(label: String, state: StepState) {
    val dotSize = MaterialTheme.dimens.icon.lg

    val bgColor by animateColorAsState(
        targetValue = when (state) {
            StepState.COMPLETED -> MaterialTheme.colorScheme.primary
            StepState.ACTIVE -> MaterialTheme.colorScheme.primary
            StepState.UPCOMING -> MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = spring(),
        label = "step-bg",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (state == StepState.ACTIVE) {
                val transition = rememberInfiniteTransition(label = "step-pulse")
                val pulseScale by transition.animateFloat(
                    initialValue = 1f,
                    targetValue = 2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "step-pulse-scale",
                )
                val pulseAlpha by transition.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
                        repeatMode = RepeatMode.Restart,
                    ),
                    label = "step-pulse-alpha",
                )
                Box(
                    modifier = Modifier
                        .size(dotSize)
                        .scale(pulseScale)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(bgColor),
                )
            }

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(bgColor),
            )
        }

        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// endregion

// region OTP Auth

@Composable
private fun AuthOtpContent(
    otpInput: String,
    onOtpChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AlohomoraOutlinedCard(
            modifier = Modifier.fillMaxWidth(0.5f),
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xxxl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.illustration / 2),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            Icons.Key,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                        )
                    }
                }

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
                    onValueChange = onOtpChange,
                    placeholder = "0000",
                    singleLine = true,
                    modifier = Modifier.width(160.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
                    AlohomoraFilledButton(
                        text = "Confirm",
                        onClick = onConfirm,
                        enabled = otpInput.length == 4,
                        uppercase = false,
                    )
                    AlohomoraOutlinedButton(
                        text = "Cancel",
                        size = AlohomoraButtonSize.SMALL,
                        onClick = onCancel,
                    )
                }
            }
        }
    }
}

// endregion

// region Suspend helpers

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

private suspend fun suspendPairDevice(
    viewModel: DevicesViewModel,
    host: String,
    port: Int,
    code: String,
): String? = suspendCancellableCoroutine { continuation ->
    viewModel.pairDevice(host, port, code) { error -> continuation.resume(error) }
}

private suspend fun suspendConnectWireless(
    viewModel: DevicesViewModel,
    host: String,
    port: Int,
): String? = suspendCancellableCoroutine { continuation ->
    viewModel.connectWireless(host, port) { error -> continuation.resume(error) }
}

private suspend fun suspendDiscoverWireless(
    viewModel: DevicesViewModel,
): WirelessDiscovery = suspendCancellableCoroutine { continuation ->
    viewModel.discoverWirelessEndpoints { result -> continuation.resume(result) }
}

/** Splits "host:port" on the last colon. Returns null when malformed. */
private fun parseHostPort(address: String): Pair<String, Int>? {
    val trimmed = address.trim()
    val separator = trimmed.lastIndexOf(':')
    if (separator <= 0 || separator == trimmed.length - 1) return null
    val port = trimmed.substring(separator + 1).toIntOrNull() ?: return null
    return trimmed.substring(0, separator) to port
}

// endregion

// region Previews

@Preview
@Composable
private fun DeviceListCardPreview() {
    AppTheme(initialIsDark = true) {
        Surface {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                DeviceListCard(
                    device = DeviceUi(
                        id = "emulator-5554",
                        state = DeviceState.DEVICE,
                        model = "Pixel 8 Pro",
                    ),
                    selected = true,
                    onClick = {},
                )
                DeviceListCard(
                    device = DeviceUi(
                        id = "R3CT20BDJFR",
                        state = DeviceState.DEVICE,
                        model = "Galaxy S24",
                    ),
                    selected = false,
                    onClick = {},
                )
                DeviceListCard(
                    device = DeviceUi(
                        id = "00008101-001A2C4E3C10001E",
                        state = DeviceState.DEVICE,
                        model = "iPhone 15 Pro",
                        platform = DevicePlatform.IOS,
                    ),
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun ConnectingContentPreview() {
    AppTheme(initialIsDark = true) {
        Surface(modifier = Modifier.size(600.dp, 400.dp)) {
            ConnectingContent(
                connectionState = DevToolsConnection.Connecting("127.0.0.1", 53999),
                onCancel = {},
            )
        }
    }
}

@Preview
@Composable
private fun AuthOtpContentPreview() {
    AppTheme(initialIsDark = true) {
        Surface(modifier = Modifier.size(600.dp, 400.dp)) {
            AuthOtpContent(
                otpInput = "12",
                onOtpChange = {},
                onConfirm = {},
                onCancel = {},
            )
        }
    }
}

@Preview
@Composable
private fun ConnectionStepperPreview() {
    AppTheme(initialIsDark = true) {
        Surface {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl).width(400.dp),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
            ) {
                ConnectionStepper(activeStep = 0)
                ConnectionStepper(activeStep = 1)
                ConnectionStepper(activeStep = 2)
            }
        }
    }
}

// endregion

// region Wireless pairing

private enum class PairMethod { QR, CODE }

private const val QR_POLL_INTERVAL_MS = 1500L
private const val QR_PAIR_ATTEMPTS = 40 // ~60s at the poll interval above

private fun randomPairingToken(): String =
    java.util.UUID.randomUUID().toString().replace("-", "").take(12)

/**
 * Android 11+ Wireless-debugging pairing, two ways:
 *
 *  - **QR code**: the desktop shows a QR that the device scans from "Pair device with QR code".
 *    The device then advertises an mDNS pairing service named after the QR; we poll `adb mdns
 *    services` for it, `adb pair`, then discover the connect service and `adb connect`. Fully
 *    automatic once scanned.
 *  - **Pairing code**: the manual fallback — type the address + 6-digit code, then the connect
 *    address. Reliable when mDNS/QR does not work on a given device or network.
 *
 * Either way, once `adb connect` succeeds the device joins `adb devices` and the normal Connect
 * flow takes over.
 */
@Composable
private fun WirelessPairingDialog(
    viewModel: DevicesViewModel,
    onConnected: (host: String, port: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var method by remember { mutableStateOf(PairMethod.QR) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Manual (code) mode.
    var pairAddress by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var connectAddress by remember { mutableStateOf("") }
    var paired by remember { mutableStateOf(false) }

    // QR mode. Service name + password are generated once; the device echoes the service name as
    // its mDNS pairing instance, which is how we match it.
    val serviceName = remember { "alohomora-${randomPairingToken()}" }
    val password = remember { randomPairingToken() }
    val qrContent = remember(serviceName, password) {
        WirelessQr.pairingPayload(serviceName, password)
    }
    var qrStatus by remember { mutableStateOf("Waiting for the device to scan…") }

    // Poll for the scanned device only while QR mode is active; leaving QR mode (or closing the
    // dialog) cancels this effect and stops polling.
    LaunchedEffect(method) {
        if (method != PairMethod.QR) return@LaunchedEffect
        error = null
        qrStatus = "Waiting for the device to scan…"
        repeat(QR_PAIR_ATTEMPTS) {
            val endpoint = suspendFindPairingEndpoint(viewModel, serviceName)
            if (endpoint != null) {
                qrStatus = "Pairing…"
                val pairError = suspendPairDevice(viewModel, endpoint.host, endpoint.port, password)
                if (pairError != null) {
                    error = pairError
                    qrStatus = "Pairing failed"
                    return@LaunchedEffect
                }
                qrStatus = "Connecting…"
                val connect = suspendDiscoverWireless(viewModel).connect
                if (connect == null) {
                    error = "Paired, but couldn't find the connect endpoint"
                    return@LaunchedEffect
                }
                val connectError = suspendConnectWireless(viewModel, connect.host, connect.port)
                if (connectError != null) {
                    error = connectError
                    qrStatus = "Connect failed"
                    return@LaunchedEffect
                }
                onConnected(connect.host, connect.port)
                return@LaunchedEffect
            }
            delay(QR_POLL_INTERVAL_MS.milliseconds)
        }
        qrStatus = "Timed out. Check the device is on the same network, or use a pairing code."
    }

    AlohomoraAlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = "Pair over Wi-Fi",
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
                    PairMethodChip("QR code", method == PairMethod.QR) {
                        method = PairMethod.QR
                        error = null
                    }
                    PairMethodChip("Pairing code", method == PairMethod.CODE) {
                        method = PairMethod.CODE
                        error = null
                    }
                }

                when (method) {
                    PairMethod.QR -> {
                        Text(
                            "On the device: Developer options → Wireless debugging → \"Pair " +
                                "device with QR code\", then scan this.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        QrCode(
                            content = qrContent,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .size(220.dp),
                        )
                        Text(
                            qrStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    PairMethod.CODE -> if (!paired) {
                        Text(
                            "On the device: Wireless debugging → \"Pair device with pairing " +
                                "code\". Enter that address and code.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AlohomoraTextField(
                            value = pairAddress,
                            onValueChange = { pairAddress = it },
                            label = "Pairing address (IP:port)",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AlohomoraTextField(
                            value = code,
                            onValueChange = { input ->
                                if (input.length <= 6 && input.all(Char::isDigit)) code = input
                            },
                            label = "Pairing code",
                            placeholder = "000000",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AlohomoraTextButton(
                            text = "Discover on network",
                            enabled = !busy,
                            onClick = {
                                scope.launch {
                                    error = null
                                    val discovery = suspendDiscoverWireless(viewModel)
                                    discovery.pairing?.let { pairAddress = it.address }
                                    discovery.connect?.let { connectAddress = it.address }
                                    if (discovery.pairing == null && discovery.connect == null) {
                                        error = "No wireless devices found on the network"
                                    }
                                }
                            },
                        )
                    } else {
                        Text(
                            "Paired. Enter the connect address from the main Wireless debugging " +
                                "screen — its port differs from the pairing port.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AlohomoraTextField(
                            value = connectAddress,
                            onValueChange = { connectAddress = it },
                            label = "Connect address (IP:port)",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            when {
                // QR pairing is automatic — nothing to confirm.
                method == PairMethod.QR -> Unit

                !paired -> AlohomoraTextButton(
                    text = if (busy) "Pairing…" else "Pair",
                    enabled = !busy && parseHostPort(pairAddress) != null && code.length == 6,
                    onClick = {
                        val (host, port) = parseHostPort(pairAddress) ?: return@AlohomoraTextButton
                        scope.launch {
                            busy = true
                            error = null
                            val err = suspendPairDevice(viewModel, host, port, code)
                            if (err == null) {
                                paired = true
                                if (connectAddress.isBlank()) {
                                    suspendDiscoverWireless(viewModel).connect
                                        ?.let { connectAddress = it.address }
                                }
                            } else {
                                error = err
                            }
                            busy = false
                        }
                    },
                )

                else -> AlohomoraTextButton(
                    text = if (busy) "Connecting…" else "Connect",
                    enabled = !busy && parseHostPort(connectAddress) != null,
                    onClick = {
                        val (host, port) = parseHostPort(connectAddress)
                            ?: return@AlohomoraTextButton
                        scope.launch {
                            busy = true
                            error = null
                            val err = suspendConnectWireless(viewModel, host, port)
                            busy = false
                            if (err == null) onConnected(host, port) else error = err
                        }
                    },
                )
            }
        },
        dismissButton = {
            AlohomoraTextButton(text = "Cancel", enabled = !busy, onClick = onDismiss)
        },
    )
}

@Composable
private fun PairMethodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        AlohomoraFilledButton(
            text = label,
            onClick = onClick,
            size = AlohomoraButtonSize.SMALL,
            uppercase = false,
        )
    } else {
        AlohomoraOutlinedButton(
            text = label,
            onClick = onClick,
            size = AlohomoraButtonSize.SMALL,
            uppercase = false,
        )
    }
}

private suspend fun suspendFindPairingEndpoint(
    viewModel: DevicesViewModel,
    serviceName: String,
): WirelessEndpoint? = suspendCancellableCoroutine { continuation ->
    viewModel.findPairingEndpoint(serviceName) { endpoint -> continuation.resume(endpoint) }
}

// endregion
