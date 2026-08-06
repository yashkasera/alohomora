package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.app.isClearShortcut
import io.github.yashkasera.alohomora.desktop.app.isModifierKeyOnly
import io.github.yashkasera.alohomora.desktop.app.isScreenshotShortcut
import io.github.yashkasera.alohomora.desktop.app.matchesNavigation
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.CommandPalette
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.HelpDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.OtpPromptDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.buildCommandActions
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.AdbToolsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.CachePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ConfigPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DashboardContent
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DatabasePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ErrorsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.GitHistoryPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.LogcatPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TraceWaterfallSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TracesPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TrafficDetailsSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TrafficPanel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TracesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.TrafficViewModel
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.icons.Alohomora
import io.github.yashkasera.alohomora.ui.icons.Android
import io.github.yashkasera.alohomora.ui.icons.Apple
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun DevToolsDesktopApp(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    cacheViewModel: CacheViewModel,
    tracesViewModel: TracesViewModel,
    eventsViewModel: EventsViewModel,
    trafficViewModel: TrafficViewModel,
    initialDeviceId: String? = null,
    showHelp: Boolean = false,
    onShowHelp: () -> Unit = {},
    onDismissHelp: () -> Unit = {},
    showCommandPalette: Boolean = false,
    onDismissCommandPalette: () -> Unit = {},
    onToggleTheme: () -> Unit = {},
    isDark: Boolean = true,
    onDisconnectWindow: () -> Unit,
) {
    var activeSection by remember { mutableStateOf(DesktopSection.Traffic) }

    val devices by devicesViewModel.devices.collectAsState()
    val adbCommandHistory by devicesViewModel.adbCommandHistory.collectAsState()
    val devToolsState by devToolsViewModel.uiState.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val deviceError by devToolsViewModel.deviceError.collectAsState()

    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedTrafficForSheet by remember { mutableStateOf<TrafficEntry?>(null) }
    // Read from the view model rather than held here: the sheet's whole state (selection, collapse,
    // split) lives there, so a second copy of "which trace is open" could only ever disagree.
    val selectedTraceId by tracesViewModel.selectedTraceId.collectAsState()
    // An id, not the Event, for the same reason as the trace above — and because markEventViewed
    // replaces the instance in the store, so a captured copy would render a stale isViewed.
    val selectedEventId by eventsViewModel.selectedEventId.collectAsState()
    var selectedDeviceId by remember(initialDeviceId) { mutableStateOf(initialDeviceId) }
    var isModifierHeld by remember { mutableStateOf(false) }

    val onlineDevices = devices.filter { it.state == DeviceState.DEVICE }
    val hasConnectedDevice = onlineDevices.isNotEmpty()

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000)
        }
    }

    LaunchedEffect(devices, selectedDeviceId) {
        val availableIds = devices.map { it.id }.toSet()
        if (selectedDeviceId.isNullOrBlank() || selectedDeviceId !in availableIds) {
            selectedDeviceId = onlineDevices.firstOrNull()?.id
        }
    }

    LaunchedEffect(selectedDeviceId, buildInfo?.packageName) {
        devicesViewModel.startDashboardPolling(selectedDeviceId, buildInfo?.packageName)
    }

    val selectedDevice = devices.firstOrNull { it.id == selectedDeviceId }
    val isConnected = devToolsState.connection is DevToolsConnection.Connected
    val isConnectedOrReconnecting = isConnected ||
        devToolsState.connection is DevToolsConnection.Reconnecting
    val connectedPlatform = selectedDevice?.platform
    val isAndroid = connectedPlatform == DevicePlatform.ANDROID

    val fallbackSection = DesktopSection.defaultFor(
        connectedPlatform ?: DevicePlatform.ANDROID,
    )

    val visibleSections = when {
        !hasConnectedDevice -> emptyList()
        connectedPlatform != null -> DesktopSection.forPlatform(connectedPlatform)
        else -> DesktopSection.entries.toList()
    }

    LaunchedEffect(activeSection, isConnectedOrReconnecting, hasConnectedDevice, connectedPlatform) {
        val gatedSections = setOf(
            DesktopSection.Traffic,
            DesktopSection.Events,
            DesktopSection.Cache,
            DesktopSection.Database,
            DesktopSection.GitHistory,
        )
        if (activeSection in gatedSections && !isConnectedOrReconnecting) {
            activeSection = fallbackSection
            devicesViewModel.setActionError("Connect a device first to open traffic, events, cache, database, and git history")
        }

        if (!hasConnectedDevice) {
            activeSection = fallbackSection
        }

        selectedDevice?.let { device ->
            if (!activeSection.isSupportedBy(device.capabilities)) {
                activeSection = fallbackSection
            }
        }
    }

    val commandActions = buildCommandActions(
        visibleSections = visibleSections,
        activeSection = activeSection,
        onSectionChange = { activeSection = it },
        isConnected = isConnected,
        packageName = buildInfo?.packageName,
        selectedDeviceId = selectedDeviceId,
        isAndroid = isAndroid,
        onToggleTheme = onToggleTheme,
        onShowHelp = {
            onDismissCommandPalette()
            onShowHelp()
        },
        onClearTraffic = { trafficViewModel.clearTraffic() },
        onClearTraces = { tracesViewModel.clearTraces() },
        onClearEvents = { eventsViewModel.clearEvents() },
        onForceStop = {
            buildInfo?.packageName?.let { pkg ->
                devicesViewModel.runCommand(selectedDeviceId, "shell am force-stop $pkg")
            }
        },
        onLaunchApp = {
            buildInfo?.packageName?.let { pkg ->
                devicesViewModel.runCommand(
                    selectedDeviceId,
                    "shell monkey -p $pkg -c android.intent.category.LAUNCHER 1",
                )
            }
        },
        onClearAppData = {
            buildInfo?.packageName?.let { pkg ->
                devicesViewModel.runCommand(selectedDeviceId, "shell pm clear $pkg")
            }
        },
        onTakeScreenshot = {
            val timestamp = System.currentTimeMillis()
            val defaultName = "alohomora_screenshot_${timestamp}.png"
            val localPath =
                pickSavePath(defaultName, "Save Screenshot", ".png") ?: return@buildCommandActions
            devicesViewModel.takeScreenshot(selectedDeviceId, localPath)
        },
        onRebootDevice = {
            devicesViewModel.runCommand(selectedDeviceId, "reboot")
        },
        onToggleWifi = {
            devicesViewModel.toggleWifi(selectedDeviceId)
        },
        onToggleMobileData = {
            devicesViewModel.toggleMobileData(selectedDeviceId)
        },
        onClearLogcat = {
            devicesViewModel.runCommand(selectedDeviceId, "logcat -c")
        },
    )

    val rootFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        rootFocus.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(rootFocus)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.isModifierKeyOnly()) {
                    isModifierHeld = event.type == KeyEventType.KeyDown
                    return@onPreviewKeyEvent false
                }

                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                if (event.key == Key.Escape) {
                    when {
                        showCommandPalette -> {
                            onDismissCommandPalette(); return@onPreviewKeyEvent true
                        }

                        showHelp -> {
                            onDismissHelp(); return@onPreviewKeyEvent true
                        }

                        selectedTrafficForSheet != null -> {
                            selectedTrafficForSheet = null; return@onPreviewKeyEvent true
                        }

                        selectedTraceId != null -> {
                            tracesViewModel.closeTrace(); return@onPreviewKeyEvent true
                        }

                        selectedEventId != null -> {
                            eventsViewModel.closeEvent(); return@onPreviewKeyEvent true
                        }
                    }
                    return@onPreviewKeyEvent false
                }

                val navIndex = event.matchesNavigation()
                if (navIndex >= 0 && navIndex < visibleSections.size) {
                    activeSection = visibleSections[navIndex]
                    return@onPreviewKeyEvent true
                }

                if (event.isClearShortcut()) {
                    when (activeSection) {
                        DesktopSection.Traffic -> trafficViewModel.clearTraffic()
                        DesktopSection.Traces -> tracesViewModel.clearTraces()
                        DesktopSection.Events -> eventsViewModel.clearEvents()
                        else -> {}
                    }
                    return@onPreviewKeyEvent true
                }

                if (event.isScreenshotShortcut() && isAndroid && !selectedDeviceId.isNullOrBlank()) {
                    val timestamp = System.currentTimeMillis()
                    val defaultName = "alohomora_screenshot_${timestamp}.png"
                    val localPath = pickSavePath(defaultName, "Save Screenshot", ".png")
                    if (localPath != null) {
                        devicesViewModel.takeScreenshot(selectedDeviceId, localPath)
                    }
                    return@onPreviewKeyEvent true
                }

                false
            },
    ) {
        val contentModifier = if (showCommandPalette)
            Modifier.fillMaxSize().blur(16.dp)
        else Modifier.fillMaxSize()

        Box(modifier = contentModifier) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.fillMaxWidth(0.2f),
                        windowInsets = WindowInsets.safeContent,
                    ) {
                        Sidebar(
                            connection = devToolsState.connection,
                            activeSection = activeSection,
                            devices = devices,
                            selectedDeviceId = selectedDeviceId,
                            hasConnectedDevice = hasConnectedDevice,
                            onDisconnect = onDisconnectWindow,
                            onSectionClick = { activeSection = it },
                            isModifierHeld = isModifierHeld,
                            visibleSections = visibleSections,
                        )
                    }
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (!hasConnectedDevice) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(hostState = devicesViewModel.snackbarHostState) },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ) {
                            NoDevicePanel(onRefresh = { devicesViewModel.refreshDevices() })
                        }
                    } else {
                        when (activeSection) {
                            DesktopSection.Dashboard -> DashboardContent(
                                devToolsViewModel = devToolsViewModel,
                                devicesViewModel = devicesViewModel,
                                selectedDevice = selectedDevice,
                                isRecording = isRecording,
                                onTakeScreenshot = screenshot@{
                                    val timestamp = System.currentTimeMillis()
                                    val defaultName = "alohomora_screenshot_${timestamp}.png"
                                    val localPath =
                                        pickSavePath(defaultName, "Save Screenshot", ".png")
                                            ?: return@screenshot
                                    devicesViewModel.takeScreenshot(selectedDeviceId, localPath)
                                },
                                onRecordScreen = record@{
                                    if (!isRecording) {
                                        val timestamp = System.currentTimeMillis()
                                        val defaultName = "alohomora_record_${timestamp}.mp4"
                                        val localPath =
                                            pickSavePath(defaultName, "Save Recording", ".mp4")
                                                ?: return@record
                                        val devicePath = "/sdcard/${File(localPath).name}"
                                        recordingDevicePath = devicePath
                                        recordingLocalPath = localPath
                                        isRecording = true
                                        devicesViewModel.startScreenRecord(
                                            selectedDeviceId,
                                            devicePath,
                                        )
                                    } else {
                                        devicesViewModel.stopScreenRecord(
                                            selectedDeviceId,
                                            recordingDevicePath,
                                            recordingLocalPath,
                                        )
                                        isRecording = false
                                        recordingDevicePath = null
                                        recordingLocalPath = null
                                    }
                                },
                                onTrafficItemClick = { selectedTrafficForSheet = it },
                                onEventViewClick = {},
                                onTrafficClick = { activeSection = DesktopSection.Traffic },
                                onEventsClick = { activeSection = DesktopSection.Events },
                            )

                            DesktopSection.Logcat -> LogcatPanel(
                                devicesViewModel = devicesViewModel,
                                logcatViewModel = logcatViewModel,
                                selectedDeviceId = selectedDeviceId,
                            )

                            DesktopSection.Adb -> AdbToolsPanel(
                                devicesViewModel = devicesViewModel,
                                selectedDeviceId = selectedDeviceId,
                                adbCommandHistory = adbCommandHistory,
                                buildInfo = buildInfo,
                            )

                            DesktopSection.Traffic -> TrafficPanel(
                                trafficViewModel = trafficViewModel,
                                onLogClick = { selectedTrafficForSheet = it },
                            )

                            DesktopSection.Traces -> TracesPanel(
                                tracesViewModel = tracesViewModel,
                                onTraceClick = tracesViewModel::openTrace,
                            )

                            DesktopSection.Events -> EventsPanel(eventsViewModel = eventsViewModel)
                            DesktopSection.Cache -> CachePanel(cacheViewModel = cacheViewModel)
                            DesktopSection.Errors -> ErrorsPanel(devToolsViewModel = devToolsViewModel)
                            DesktopSection.Config -> ConfigPanel(devToolsViewModel = devToolsViewModel)
                            DesktopSection.GitHistory -> GitHistoryPanel(devToolsViewModel = devToolsViewModel)
                            DesktopSection.Database -> DatabasePanel(databaseViewModel = databaseViewModel)
                        }
                    }

                    AnimatedVisibility(
                        visible = devToolsState.connection is DevToolsConnection.Reconnecting,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        val attempt = (devToolsState.connection as? DevToolsConnection.Reconnecting)?.attempt ?: 1
                        ReconnectingBanner(attempt = attempt)
                    }

                    deviceError?.let { error ->
                        DeviceErrorBanner(
                            message = error,
                            onDismiss = { devToolsViewModel.dismissDeviceError() },
                            modifier = Modifier.align(Alignment.TopCenter),
                        )
                    }

                    val connection = devToolsState.connection
                    if (connection is DevToolsConnection.AwaitingAuth && connection.otpRequired) {
                        OtpPromptDialog(
                            onSubmit = { devToolsViewModel.submitOtp(it) },
                            onCancel = { devToolsViewModel.disconnect() },
                        )
                    }

                    if (devToolsState.switching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                                .clickable(indication = null, interactionSource = null) {},
                            contentAlignment = Alignment.Center,
                        ) {
                            AlohomoraCircularProgressIndicator()
                        }
                    }
                }
            }

            TrafficDetailsSideSheet(
                traffic = selectedTrafficForSheet,
                devToolsViewModel = devToolsViewModel,
                onDismiss = { selectedTrafficForSheet = null },
            )

            TraceWaterfallSideSheet(
                tracesViewModel = tracesViewModel,
                onDismiss = tracesViewModel::closeTrace,
            )

            EventDetailsSideSheet(
                eventsViewModel = eventsViewModel,
                devToolsViewModel = devToolsViewModel,
                onDismiss = eventsViewModel::closeEvent,
            )
        }

        if (showCommandPalette) {
            CommandPalette(
                actions = commandActions,
                onDismiss = onDismissCommandPalette,
            )
        }
    }

    if (showHelp) {
        HelpDialog(
            visibleSections = visibleSections,
            actions = commandActions,
            isDark = isDark,
            onDismiss = onDismissHelp,
        )
    }
}

@Composable
private fun NoDevicePanel(
    onRefresh: () -> Unit,
) {
    EmptyState(
        icon = Icons.HardDrive,
        title = "No device connected",
        subtitle = "Connect an Android device over USB or adb tcpip, or an iPhone over USB, then refresh.",
        action = {
            AlohomoraOutlinedButton(text = "Refresh devices", onClick = onRefresh)
        },
    )
}

@Composable
fun ColumnScope.Sidebar(
    activeSection: DesktopSection,
    onDisconnect: () -> Unit,
    onSectionClick: (DesktopSection) -> Unit,
    connection: DevToolsConnection,
    devices: List<DeviceUi>,
    selectedDeviceId: String?,
    hasConnectedDevice: Boolean,
    isModifierHeld: Boolean = false,
    visibleSections: List<DesktopSection> = emptyList(),
) {
    Row(
        modifier = Modifier
            .padding(top = MaterialTheme.dimens.margin.xxl)
            .padding(horizontal = MaterialTheme.dimens.margin.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Alohomora,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
        Text(
            "Alohomora.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    SidebarConnectionCard(
        connection = connection,
        devices = devices,
        selectedDeviceId = selectedDeviceId,
        hasConnectedDevice = hasConnectedDevice,
        onDisconnect = onDisconnect,
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        contentPadding = PaddingValues(MaterialTheme.dimens.margin.lg)
    ) {
        itemsIndexed(visibleSections, key = { _, section -> section.title }) { index, section ->
            NavigationDrawerItem(
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (isModifierHeld && index < 9) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(MaterialTheme.dimens.corner.small),
                                    )
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                },
                selected = activeSection == section,
                icon = {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                },
                onClick = { onSectionClick(section) },
            )
        }
    }
}

@Composable
private fun SidebarConnectionCard(
    connection: DevToolsConnection,
    devices: List<DeviceUi>,
    selectedDeviceId: String?,
    hasConnectedDevice: Boolean,
    onDisconnect: () -> Unit,
) {
    val onlineDevices = devices.filter { it.state == DeviceState.DEVICE }
    val selectedOnlineDevice =
        onlineDevices.firstOrNull { it.id == selectedDeviceId } ?: onlineDevices.firstOrNull()

    Column(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.dimens.margin.lg)
            .border(
                MaterialTheme.dimens.stroke.small,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.small,
            )
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(MaterialTheme.dimens.margin.md),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!hasConnectedDevice || selectedOnlineDevice == null) {
            Text(
                text = "No online devices found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotState = when (connection) {
                    DevToolsConnection.Disconnected -> ConnectionDotState.Disconnected
                    is DevToolsConnection.Connecting -> ConnectionDotState.Reconnecting
                    is DevToolsConnection.AwaitingAuth -> ConnectionDotState.Reconnecting
                    is DevToolsConnection.Connected -> ConnectionDotState.Connected
                    is DevToolsConnection.Reconnecting -> ConnectionDotState.Reconnecting
                    is DevToolsConnection.Failed -> ConnectionDotState.Disconnected
                }
                ConnectionStatusDot(state = dotState)
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                val connectionText = when (connection) {
                    DevToolsConnection.Disconnected -> "Disconnected"
                    is DevToolsConnection.Connecting -> "Connecting ${connection.host}:${connection.port}"
                    is DevToolsConnection.AwaitingAuth -> "Waiting for OTP"
                    is DevToolsConnection.Connected -> "Connected"
                    is DevToolsConnection.Reconnecting ->
                        "Device asleep — reconnecting (${connection.attempt})"

                    is DevToolsConnection.Failed -> "Failed: ${connection.reason}"
                }
                Text(
                    connectionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MaterialTheme.dimens.corner.medium))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector =
                        if (selectedOnlineDevice.platform.isIos)
                            Icons.Apple
                        else Icons.Android,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(MaterialTheme.dimens.margin.sm)
                        .size(24.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedOnlineDevice.model ?: selectedOnlineDevice.id,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedOnlineDevice.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (connection is DevToolsConnection.Connected || connection is DevToolsConnection.Reconnecting) {
                    IconButton(onClick = onDisconnect) {
                        Icon(Icons.X, contentDescription = "Disconnect")
                    }
                }
            }
        }

    }
}

/**
 * Advisory banner for a command the device could not serve.
 *
 * Deliberately dismissible and non-blocking: the session is still live and every other panel keeps
 * working, so this must not become a modal that hides the console. It exists because the failure it
 * reports used to be completely silent on this side.
 */
@Composable
private fun DeviceErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(MaterialTheme.dimens.margin.md)
            .widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = MaterialTheme.dimens.margin.xs,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun ReconnectingBanner(
    attempt: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(MaterialTheme.dimens.margin.md)
            .widthIn(max = 480.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = MaterialTheme.dimens.margin.xs,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            ConnectionStatusDot(state = ConnectionDotState.Reconnecting)
            Text(
                text = "Reconnecting (attempt $attempt)...",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
