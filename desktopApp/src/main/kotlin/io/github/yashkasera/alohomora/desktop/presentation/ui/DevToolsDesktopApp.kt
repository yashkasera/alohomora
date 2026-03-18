package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
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
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.brand
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEVTOOLS_PORT = 53999

@Suppress("UNUSED_PARAMETER")
@Composable
fun DevToolsDesktopApp(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    prefsViewModel: PrefsViewModel,
    host: String,
    port: String,
) {
    var activeSection by remember { mutableStateOf(DesktopSection.Dashboard) }

    val devices by devicesViewModel.devices.collectAsState()
    val selectedDeviceId by devicesViewModel.selectedDeviceId.collectAsState()
    val adbCommandHistory by devicesViewModel.adbCommandHistory.collectAsState()
    val devToolsState by devToolsViewModel.uiState.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()

    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }
    var selectedTraceForDrawer by remember { mutableStateOf<TraceEntry?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000)
        }
    }

    LaunchedEffect(selectedDeviceId, buildInfo?.packageName) {
        devicesViewModel.startDashboardPolling(selectedDeviceId, buildInfo?.packageName)
    }

    val selectedDevice = devices.firstOrNull { it.id == selectedDeviceId } ?: devices.firstOrNull()
    val isConnected = devToolsState.connection is DevToolsConnection.Connected

    LaunchedEffect(activeSection, isConnected) {
        val gatedSections = setOf(
            DesktopSection.Traces,
            DesktopSection.TelemetryEvents,
            DesktopSection.Preferences,
            DesktopSection.Database,
            DesktopSection.Chronicle,
        )
        if (activeSection in gatedSections && !isConnected) {
            activeSection = DesktopSection.Logcat
            devicesViewModel.setActionError("Connect a device first to open traces, telemetry, preferences, config, and chronicle")
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet(
                    modifier = Modifier.fillMaxWidth(0.25f),
                    windowInsets = WindowInsets.safeContent,
                ) {
                    Sidebar(
                        connection = devToolsState.connection,
                        activeSection = activeSection,
                        onDisconnect = {
                            val numericPort = port.toIntOrNull() ?: DEVTOOLS_PORT
                            scope.launch {
                                devicesViewModel.disconnectHost(host, numericPort)
                                devicesViewModel.deactivateDevice(numericPort)
                                devToolsViewModel.disconnect()
                            }
                        },
                        onSectionClick = { activeSection = it },
                    )
                }
            },
        ) {
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
                         devicesViewModel.takeScreenshot(
                             selectedDeviceId,
                             devicePath,
                             localPath,
                         )
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
                     onEventViewClick = {
                     },
                     onTracesClick = {
                         activeSection = DesktopSection.Traces
                     },
                     onEventsClick = {
                         activeSection = DesktopSection.TelemetryEvents
                     },
                 )

 //                DesktopSection.Builds -> CurrentBuildCard(
 //                    buildInfo = buildInfo,
 //                    modifier = Modifier.fillMaxWidth(),
 //                )

                 DesktopSection.Logcat -> LogcatPanel(
                     devicesViewModel = devicesViewModel,
                     logcatViewModel = logcatViewModel,
                 )

                 DesktopSection.Adb -> AdbToolsPanel(
                     devicesViewModel = devicesViewModel,
                     selectedDeviceId = selectedDeviceId,
                     adbCommandHistory = adbCommandHistory,
                     buildInfo = buildInfo,
                 )

                 DesktopSection.Traces ->
                     ApiLogsPanel(
                         devToolsViewModel = devToolsViewModel,
                         onLogClick = { selectedTraceForDrawer = it },
                     )

                 DesktopSection.TelemetryEvents ->
                     EventsPanel(devToolsViewModel = devToolsViewModel)

                 DesktopSection.Preferences ->
                     PreferencesPanel(prefsViewModel = prefsViewModel)

                 DesktopSection.Chronicle ->
                     ChroniclePanel(devToolsViewModel = devToolsViewModel)

                 DesktopSection.Database ->
                     DatabasePanel(databaseViewModel = databaseViewModel)

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
fun ColumnScope.Sidebar(
    activeSection: DesktopSection,
    onDisconnect: () -> Unit,
    onSectionClick: (DesktopSection) -> Unit,
    connection: DevToolsConnection,
) {
    Row(
        modifier = Modifier
            .padding(top = 24.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.brand),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Alohomora.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dotState = when (connection) {
            DevToolsConnection.Disconnected -> ConnectionDotState.Disconnected
            is DevToolsConnection.Connecting -> ConnectionDotState.Reconnecting
            is DevToolsConnection.Connected -> ConnectionDotState.Connected
            is DevToolsConnection.Failed -> ConnectionDotState.Disconnected
        }
        ConnectionStatusDot(state = dotState)
        Spacer(modifier = Modifier.width(8.dp))
        val connectionText = when (connection) {
            DevToolsConnection.Disconnected -> "Disconnected"
            is DevToolsConnection.Connecting -> "Connecting ${connection.host}:${connection.port}"
            is DevToolsConnection.Connected -> "Connected ${connection.host}:${connection.port}"
            is DevToolsConnection.Failed -> "Failed: ${connection.reason}"
        }
        Text(
            connectionText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
        )
    }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DesktopSection.entries.forEach { section ->
            NavigationDrawerItem(
                label = {
                    Text(text = section.title)
                },
                selected = activeSection == section,
                icon = {
                    Icon(
                        section.icon,
                        contentDescription = null,
                    )
                },
                onClick = { onSectionClick(section) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        NavigationDrawerItem(
            label = {
                Text(text = "Disconnect")
            },
            icon = {
                Icon(
                    Icons.X,
                    contentDescription = null,
                )
            },
            selected = false,
            onClick = onDisconnect,
        )
    }
}

