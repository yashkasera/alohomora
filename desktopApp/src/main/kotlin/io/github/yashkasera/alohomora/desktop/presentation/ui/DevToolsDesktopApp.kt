package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.presentation.model.DashboardUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.AdbToolsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ApiLogsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ChroniclePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ConfigPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.LogcatPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.PreferencesPanel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PrefsViewModel
import io.github.yashkasera.alohomora.desktop.util.pickSavePath
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.CanvasSuccessGreen
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import io.github.yashkasera.alohomora.ui.theme.success
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEVTOOLS_PORT = 53999
private const val NETWORK_WINDOW_MS = 5 * 60 * 1000L
private val buildTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm").withZone(ZoneId.systemDefault())

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
    val dashboard by devicesViewModel.dashboardState.collectAsState()
    val adbError by devicesViewModel.error.collectAsState()
    val adbCommandHistory by devicesViewModel.adbCommandHistory.collectAsState()
    val devToolsState by devToolsViewModel.uiState.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val events by devToolsViewModel.events.collectAsState()
    val apiLogs by devToolsViewModel.apiLogs.collectAsState()

    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var recordingDevicePath by remember { mutableStateOf<String?>(null) }
    var recordingLocalPath by remember { mutableStateOf<String?>(null) }

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
            DesktopSection.Config,
            DesktopSection.Chronicle,
        )
        if (activeSection in gatedSections && !isConnected) {
            activeSection = DesktopSection.Logcat
            devicesViewModel.setActionError("Connect a device first to open traces, telemetry, preferences, config, and chronicle")
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            Sidebar(
                connection = devToolsState.connection,
                activeSection = activeSection,
                onDisconnect = {
                    val numericPort = port.toIntOrNull() ?: DEVTOOLS_PORT
                    scope.launch {
                        devicesViewModel.disconnectHost(host, numericPort)
                        devicesViewModel.deactivateDevice(numericPort)
                        devToolsViewModel.disconnect()
                        devicesViewModel.setActionMessage("Disconnected")
                    }
                },
                onSectionClick = { activeSection = it },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                Header(
                    connection = devToolsState.connection,
                    onRefresh = { devicesViewModel.refreshDevices() },
                )
                Spacer(modifier = Modifier.height(32.dp))

                when (activeSection) {
                    DesktopSection.Dashboard -> DashboardContent(
                        selectedDevice = selectedDevice,
                        dashboard = dashboard,
                        apiLogs = apiLogs,
                        buildInfo = buildInfo,
                        events = events.takeLast(3).asReversed().map {
                            EventItemData(
                                title = it.name,
                                subtitle = it.properties?.toString() ?: "No properties",
                                time = it.time.toString(),
                                dotColor = MaterialTheme.colorScheme.success,
                            )
                        },
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
                        connection = devToolsState.connection,
                        actionMessage = dashboard.actionMessage,
                        actionError = dashboard.actionError ?: adbError,
                    )

                    DesktopSection.Builds -> CurrentBuildCard(
                        buildInfo = buildInfo,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    DesktopSection.Logcat -> LogcatPanel(
                        devicesViewModel = devicesViewModel,
                        logcatViewModel = logcatViewModel,
                    )

                    DesktopSection.Adb -> AdbToolsPanel(
                        devicesViewModel = devicesViewModel,
                        selectedDeviceId = selectedDeviceId,
                        adbCommandHistory = adbCommandHistory,
                        buildInfo = buildInfo,
                        actionMessage = dashboard.actionMessage,
                        actionError = dashboard.actionError ?: adbError,
                    )

                    DesktopSection.Traces ->
                        ApiLogsPanel(devToolsViewModel = devToolsViewModel)

                    DesktopSection.TelemetryEvents ->
                        EventsPanel(devToolsViewModel = devToolsViewModel)

                    DesktopSection.Preferences -> SectionContentCard(
                        title = "Preferences",
                        subtitle = "Live keys and values from connected app",
                    ) {
                        PreferencesPanel(prefsViewModel = prefsViewModel)
                    }

                    DesktopSection.Config -> SectionContentCard(
                        title = "Config",
                        subtitle = "Build metadata from connected app",
                    ) {
                        ConfigPanel(devToolsViewModel = devToolsViewModel)
                    }

                    DesktopSection.Chronicle ->
                        ChroniclePanel(devToolsViewModel = devToolsViewModel)
                }

                Spacer(modifier = Modifier.weight(1f))
                Footer()
            }
        }
    }
}

@Composable
fun Sidebar(
    activeSection: DesktopSection,
    onDisconnect: () -> Unit,
    onSectionClick: (DesktopSection) -> Unit,
    connection: DevToolsConnection,
) {
    Column(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A56DB)),
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
                .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
                .fillMaxWidth()
                .padding(8.dp),
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
            Text(connectionText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }


        Spacer(modifier = Modifier.height(40.dp))

        DesktopSection.entries.forEach { section ->
            SidebarItem(
                section = section,
                isSelected = activeSection == section,
                onClick = { onSectionClick(section) },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onDisconnect)
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlohomoraIcon(
                imageVector = Icons.X,
                contentDescription = null,
                tint = Color.DarkGray,
                size = 24.dp,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Disconnect",
                color = Color.DarkGray,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun SidebarItem(section: DesktopSection, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.DarkGray

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlohomoraIcon(
            section.icon,
            contentDescription = null,
            tint = contentColor,
            size = 20.dp,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            section.title,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun Header(
    connection: DevToolsConnection,
    onRefresh: () -> Unit,
) {
    val isDarkTheme = LocalThemeIsDark.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        AlohomoraFilterChip(
            selected = isDarkTheme.value,
            onClick = { isDarkTheme.value = !isDarkTheme.value },
            label = "Dark Mode",
        )
    }
}

@Composable
fun DashboardContent(
    selectedDevice: DeviceUi?,
    dashboard: DashboardUiState,
    apiLogs: List<TraceEntry>,
    buildInfo: BuildInfo?,
    events: List<EventItemData>,
    recordLabel: String,
    onTakeScreenshot: () -> Unit,
    onRecordScreen: () -> Unit,
    onClearAppData: () -> Unit,
    onRestartAdb: () -> Unit,
    connection: DevToolsConnection,
    actionMessage: String?,
    actionError: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DeviceInfoCard(
                selectedDevice = selectedDevice,
                dashboard = dashboard,
                connection = connection,
                modifier = Modifier.weight(2f),
            )
            CurrentBuildCard(buildInfo = buildInfo, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            RecentEventsCard(events = events, modifier = Modifier.weight(2f).fillMaxHeight())
            NetworkTrafficLogsCard(
                apiLogs = apiLogs,
                modifier = Modifier.weight(2f).fillMaxHeight(),
            )
            QuickActionsCard(
                onTakeScreenshot = onTakeScreenshot,
                onRecordScreen = onRecordScreen,
                onClearAppData = onClearAppData,
                onRestartAdb = onRestartAdb,
                recordLabel = recordLabel,
                modifier = Modifier.weight(1f).fillMaxHeight(),
            )
        }

        if (!actionMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(actionMessage, color = CanvasSuccessGreen)
        }
        if (!actionError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(actionError, color = Color(0xFFC62828))
        }
    }
}

@Composable
fun DeviceInfoCard(
    selectedDevice: DeviceUi?,
    dashboard: DashboardUiState,
    connection: DevToolsConnection,
    modifier: Modifier = Modifier,
) {
    val dotColor = when (connection) {
        is DevToolsConnection.Connected -> CanvasSuccessGreen
        is DevToolsConnection.Connecting -> Color(0xFFF59E0B)
        is DevToolsConnection.Failed -> Color(0xFFC62828)
        DevToolsConnection.Disconnected -> Color.LightGray
    }
    val connectionText = when (connection) {
        is DevToolsConnection.Connected -> "CONNECTED VIA ADB WI-FI"
        is DevToolsConnection.Connecting -> "CONNECTING"
        is DevToolsConnection.Failed -> "FAILED"
        DevToolsConnection.Disconnected -> "DISCONNECTED"
    }

    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    connectionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = dotColor,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                selectedDevice?.model ?: selectedDevice?.id ?: "No Device",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Android ${dashboard.androidVersion} • API Level ${dashboard.apiLevel}",
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                InfoItem("BATTERY", dashboard.batteryPercent, dashboard.batteryStatus)
                InfoItem("MEMORY", dashboard.memoryUsageGb, "/ ${dashboard.memoryTotalGb} GB")
                InfoItem(
                    "LATENCY",
                    "${dashboard.latencyMs}ms",
                    null,
                    valueColor = Color(0xFF1A56DB),
                )
            }
        }
    }
}

@Composable
fun InfoItem(label: String, value: String, subValue: String?, valueColor: Color = Color.Black) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
fun CurrentBuildCard(
    buildInfo: BuildInfo?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Current Build", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    buildInfo?.versionName?.ifBlank { "-" } ?: "-",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    buildInfo?.versionCode?.toString()?.let { "($it)" } ?: "",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                buildInfo?.projectName ?: "No build info",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    buildInfo?.branch?.let { "Branch: $it" } ?: "Branch: -",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
                Text(
                    buildInfo?.commitSha?.take(7)?.let { "Commit: $it" } ?: "Commit: -",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val buildMeta = listOfNotNull(
                    buildInfo?.variantName?.takeIf { it.isNotBlank() },
                    buildInfo?.buildType?.takeIf { it.isNotBlank() },
                    buildInfo?.flavorName?.takeIf { it.isNotBlank() },
                ).joinToString(" • ").ifBlank { "-" }
                Text(
                    buildMeta,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
                Text(
                    buildInfo?.buildTimestampUtc?.let {
                        buildTimeFormatter.format(
                            Instant.ofEpochMilli(
                                it,
                            ),
                        )
                    } ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                )
            }
            if (buildInfo != null && buildInfo.isDirty) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Working tree dirty",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(160.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value.ifBlank { "-" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                }
            }
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(40.dp).background(Color(0xFFF9FAFB)))
        }
    }
}

@Composable
fun RecentEventsCard(events: List<EventItemData>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "VIEW ALL LOGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1A56DB),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val fallback = listOf(
                EventItemData(
                    "No_Events",
                    "Waiting for telemetry stream",
                    "--",
                    Color.LightGray,
                ),
            )
            val rendered = events.ifEmpty { fallback }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(rendered) { event ->
                    EventRow(event)
                    if (event != rendered.last()) HorizontalDivider(
                        modifier = Modifier.padding(top = 16.dp),
                        thickness = 0.5.dp,
                        color = Color(0xFFF3F4F6),
                    )
                }
            }
        }
    }
}

data class EventItemData(
    val title: String,
    val subtitle: String,
    val time: String,
    val dotColor: Color,
)

@Composable
fun EventRow(event: EventItemData) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(event.dotColor))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(event.subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        Text(event.time, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
    }
}

@Composable
fun NetworkTrafficLogsCard(apiLogs: List<TraceEntry>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Network Traffic Logs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "VIEW ALL LOGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF1A56DB),
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            val rendered = apiLogs.takeLast(6).asReversed()
            if (rendered.isEmpty()) {
                Text(
                    "No network traces yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(rendered) { log ->
                        Column {
                            Text(
                                text = "${log.method ?: "?"} ${log.path ?: log.url ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = "status=${log.status ?: "-"}  duration=${log.duration ?: 0}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray,
                            )
                            if (!log.message.isNullOrBlank()) {
                                Text(
                                    text = log.message ?: "",
                                    maxLines = 2,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                )
                            }
                        }
                        if (log != rendered.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 12.dp),
                                thickness = 0.5.dp,
                                color = Color(0xFFF3F4F6),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionsCard(
    onTakeScreenshot: () -> Unit,
    onRecordScreen: () -> Unit,
    onClearAppData: () -> Unit,
    onRestartAdb: () -> Unit,
    recordLabel: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            QuickActionButton("Take Screenshot", onTakeScreenshot)
            QuickActionButton(recordLabel, onRecordScreen)
            QuickActionButton("Clear App Data", onClearAppData)
            QuickActionButton("Restart ADB", onRestartAdb)
        }
    }
}

@Composable
fun QuickActionButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun SectionContentCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder(),
        ) {
            content()
        }
    }
}

@Composable
fun Footer() {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            "© 2023 Android Unified Console.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Documentation", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Support", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text("Privacy", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
private fun AlohomoraIcon(
    imageVector: ImageVector,
    contentDescription: String? = null,
    size: Dp,
    tint: Color = Color.Black,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        tint = tint,
    )
}

