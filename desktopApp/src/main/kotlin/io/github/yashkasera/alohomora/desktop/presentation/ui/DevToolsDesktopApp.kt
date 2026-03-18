package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.AdbToolsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ApiLogsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ChroniclePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DashboardContent
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DatabasePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.LogcatPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.PreferencesPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.TraceDetailsSideModal
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PrefsViewModel
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.brand
import io.github.yashkasera.alohomora.ui.theme.dimens
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DevToolsDesktopApp(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    prefsViewModel: PrefsViewModel,
    initialDeviceId: String? = null,
    onDisconnectWindow: () -> Unit,
) {
    var activeSection by remember { mutableStateOf(DesktopSection.Dashboard) }

    val devices by devicesViewModel.devices.collectAsState()
    val adbCommandHistory by devicesViewModel.adbCommandHistory.collectAsState()
    val devToolsState by devToolsViewModel.uiState.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()

    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedTraceForDrawer by remember { mutableStateOf<TraceEntry?>(null) }
    var selectedDeviceId by remember(initialDeviceId) { mutableStateOf(initialDeviceId) }

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

    LaunchedEffect(activeSection, isConnected, hasConnectedDevice) {
        val gatedSections = setOf(
            DesktopSection.Traces,
            DesktopSection.TelemetryEvents,
            DesktopSection.Preferences,
            DesktopSection.Database,
            DesktopSection.Chronicle,
        )
        if (activeSection in gatedSections && !isConnected) {
            activeSection = DesktopSection.Dashboard
            devicesViewModel.setActionError("Connect a device first to open traces, telemetry, preferences, config, and chronicle")
        }

        if (!hasConnectedDevice) {
            activeSection = DesktopSection.Dashboard
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        onRefreshDevices = { devicesViewModel.refreshDevices() },
                        onDisconnect = onDisconnectWindow,
                        onSectionClick = { activeSection = it },
                    )
                }
            },
        ) {
            if (!hasConnectedDevice) {
                NoDevicePanel(onRefresh = { devicesViewModel.refreshDevices() })
            } else {
                when (activeSection) {
                    DesktopSection.Dashboard -> DashboardContent(
                        devToolsViewModel = devToolsViewModel,
                        devicesViewModel = devicesViewModel,
                        selectedDevice = selectedDevice,
                        recordLabel = if (isRecording) "Stop Recording" else "Start Recording",
                        onTakeScreenshot = screenshot@{
                            val timestamp = System.currentTimeMillis()
                            val defaultName = "alohomora_screenshot_${timestamp}.png"
                            val localPath = pickSavePath(defaultName, "Save Screenshot", ".png")
                                ?: return@screenshot
                            val devicePath = "/sdcard/${File(localPath).name}"
                            devicesViewModel.takeScreenshot(selectedDeviceId, devicePath, localPath)
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
                                devicesViewModel.startScreenRecord(selectedDeviceId, devicePath)
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
                        onClearAppData = {
                            val packageName = buildInfo?.packageName
                            if (packageName.isNullOrBlank()) {
                                devicesViewModel.setActionError("Build package name unavailable")
                                return@DashboardContent
                            }
                            devicesViewModel.runCommand(
                                selectedDeviceId,
                                "shell pm clear $packageName",
                            )
                            devicesViewModel.setActionMessage("App data clear command sent")
                        },
                        onRestartAdb = {
                            scope.launch {
                                devicesViewModel.restartAdb { error ->
                                    if (error == null) devicesViewModel.setActionMessage("ADB restarted")
                                    else devicesViewModel.setActionError(error)
                                }
                            }
                        },
                        onApiLogClick = { selectedTraceForDrawer = it },
                        connection = devToolsState.connection,
                        onEventViewClick = {},
                        onTracesClick = { activeSection = DesktopSection.Traces },
                        onEventsClick = { activeSection = DesktopSection.TelemetryEvents },
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

                    DesktopSection.Traces -> ApiLogsPanel(
                        devToolsViewModel = devToolsViewModel,
                        onLogClick = { selectedTraceForDrawer = it },
                    )

                    DesktopSection.TelemetryEvents -> EventsPanel(devToolsViewModel = devToolsViewModel)
                    DesktopSection.Preferences -> PreferencesPanel(prefsViewModel = prefsViewModel)
                    DesktopSection.Chronicle -> ChroniclePanel(devToolsViewModel = devToolsViewModel)
                    DesktopSection.Database -> DatabasePanel(databaseViewModel = databaseViewModel)
                }
            }
        }

        TraceDetailsSideModal(
            trace = selectedTraceForDrawer,
            devToolsViewModel = devToolsViewModel,
            onDismiss = { selectedTraceForDrawer = null },
        )
    }
}

@Composable
private fun NoDevicePanel(
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.dimens.margin.xxl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No device connected", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Connect an Android device via USB or adb tcpip, then refresh devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
        OutlinedButton(onClick = onRefresh) {
            Text("Refresh devices")
        }
    }
}

@Composable
fun Sidebar(
    activeSection: DesktopSection,
    onDisconnect: () -> Unit,
    onSectionClick: (DesktopSection) -> Unit,
    connection: DevToolsConnection,
    devices: List<DeviceUi>,
    selectedDeviceId: String?,
    hasConnectedDevice: Boolean,
    onRefreshDevices: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(top = MaterialTheme.dimens.margin.xxl)
            .padding(horizontal = MaterialTheme.dimens.margin.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.icon.standard)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.brand),
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
        onRefreshDevices = onRefreshDevices,
        onDisconnect = onDisconnect,
    )

    val globalSections = emptySet<DesktopSection>()
    val visibleSections = if (hasConnectedDevice) {
        DesktopSection.entries
    } else {
        DesktopSection.entries.filter { it in globalSections }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        visibleSections.forEach { section ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.bodyMedium,
                    )
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
    onRefreshDevices: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val onlineDevices = devices.filter { it.state == DeviceState.DEVICE }
    val selectedOnlineDevice =
        onlineDevices.firstOrNull { it.id == selectedDeviceId } ?: onlineDevices.firstOrNull()

    Column(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.dimens.margin.lg)
            .border(MaterialTheme.dimens.stroke.small, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(MaterialTheme.dimens.margin.md),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dotState = when (connection) {
                DevToolsConnection.Disconnected -> ConnectionDotState.Disconnected
                is DevToolsConnection.Connecting -> ConnectionDotState.Reconnecting
                is DevToolsConnection.AwaitingAuth -> ConnectionDotState.Reconnecting
                is DevToolsConnection.Connected -> ConnectionDotState.Connected
                is DevToolsConnection.Failed -> ConnectionDotState.Disconnected
            }
            ConnectionStatusDot(state = dotState)
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            val connectionText = when (connection) {
                DevToolsConnection.Disconnected -> "Disconnected"
                is DevToolsConnection.Connecting -> "Connecting ${connection.host}:${connection.port}"
                is DevToolsConnection.AwaitingAuth -> "Waiting for OTP"
                is DevToolsConnection.Connected -> "Connected ${connection.host}:${connection.port}"
                is DevToolsConnection.Failed -> "Failed: ${connection.reason}"
            }
            Text(
                connectionText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Current Device",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            IconButton(onClick = onRefreshDevices) {
                Icon(Icons.RefreshCw, contentDescription = "Refresh devices")
            }
            if (connection is DevToolsConnection.Connected) {
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.X, contentDescription = "Disconnect")
                }
            }
        }

        if (!hasConnectedDevice || selectedOnlineDevice == null) {
            Text(
                text = "No online devices found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MaterialTheme.dimens.corner.medium))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.HardDrive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedOnlineDevice.model ?: selectedOnlineDevice.id,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = selectedOnlineDevice.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }

    }
}
