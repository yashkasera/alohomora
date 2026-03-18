package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.presentation.model.DashboardUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TelemetryItem
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TraceItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.theme.brand
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.theme.logError
import io.github.yashkasera.alohomora.ui.theme.muted
import io.github.yashkasera.alohomora.ui.theme.subtleSurfaceAlt
import io.github.yashkasera.alohomora.ui.theme.success
import io.github.yashkasera.alohomora.ui.theme.warning

@Composable
fun DashboardContent(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    selectedDevice: DeviceUi?,
    recordLabel: String,
    onTakeScreenshot: () -> Unit,
    onRecordScreen: () -> Unit,
    onClearAppData: () -> Unit,
    onRestartAdb: () -> Unit,
    onApiLogClick: (TraceEntry) -> Unit,
    onEventViewClick: (TelemetryEvent) -> Unit,
    onTracesClick: () -> Unit,
    onEventsClick: () -> Unit,
    connection: DevToolsConnection,
) {
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val events by devToolsViewModel.events.collectAsState()
    val apiLogs by devToolsViewModel.apiLogs.collectAsState()
    val dashboard by devicesViewModel.dashboardState.collectAsState()
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = devicesViewModel.snackbarHostState
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(it)
                .padding(MaterialTheme.dimens.margin.xxl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .wrapContentHeight(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
            ) {
                DeviceInfoCard(
                    selectedDevice = selectedDevice,
                    dashboard = dashboard,
                    connection = connection,
                    modifier = Modifier.weight(2f),
                )
                CurrentBuildCard(buildInfo = buildInfo, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
            ) {
                RecentEventsCard(
                    events = events,
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    onViewEvent = onEventViewClick,
                    onViewEvents = onEventsClick,
                )
                NetworkTrafficLogsCard(
                    apiLogs = apiLogs,
                    onLogClick = onApiLogClick,
                    modifier = Modifier.weight(2f).fillMaxHeight(),
                    onTracesClick = onTracesClick,
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
    OutlinedCard(
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl)) {
            Text(
                "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ){
                QuickActionButton("Take Screenshot", onTakeScreenshot)
                QuickActionButton(recordLabel, onRecordScreen)
                QuickActionButton("Clear App Data", onClearAppData)
                QuickActionButton("Restart ADB", onRestartAdb)
            }
        }
    }
}

@Composable
fun RowScope.DeviceInfoCard(
    selectedDevice: DeviceUi?,
    dashboard: DashboardUiState,
    connection: DevToolsConnection,
    modifier: Modifier = Modifier,
) {
    val dotColor = when (connection) {
        is DevToolsConnection.Connected -> MaterialTheme.colorScheme.success
        is DevToolsConnection.Connecting -> MaterialTheme.colorScheme.warning
        is DevToolsConnection.AwaitingAuth -> MaterialTheme.colorScheme.warning
        is DevToolsConnection.Failed -> MaterialTheme.colorScheme.logError
        DevToolsConnection.Disconnected -> MaterialTheme.colorScheme.muted
    }
    val connectionText = when (connection) {
        is DevToolsConnection.Connected -> "CONNECTED VIA ADB WI-FI"
        is DevToolsConnection.Connecting -> "CONNECTING"
        is DevToolsConnection.AwaitingAuth -> "AWAITING AUTH"
        is DevToolsConnection.Failed -> "FAILED"
        DevToolsConnection.Disconnected -> "DISCONNECTED"
    }

    OutlinedCard(modifier = modifier.height(200.dp)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
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
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.weight(1f))
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
                    valueColor = MaterialTheme.colorScheme.brand,
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    subValue: String?,
    valueColor: Color = Color.Unspecified,
) {
    val resolvedValueColor =
        if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = resolvedValueColor,
            )
            if (subValue != null) {
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.xs))
                Text(
                    subValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun RowScope.CurrentBuildCard(
    modifier: Modifier = Modifier,
    buildInfo: BuildInfo?,
) {
    OutlinedCard(modifier = modifier.height(200.dp)) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Current Build", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    buildInfo?.versionName?.ifBlank { "-" } ?: "-",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                Text(
                    buildInfo?.versionCode?.toString()?.let { "($it)" } ?: "",
                    color = MaterialTheme.colorScheme.muted,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Text(
                buildInfo?.projectName ?: "No build info",
                color = MaterialTheme.colorScheme.secondary,
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
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    buildInfo?.commitSha?.take(7)?.let { "Commit: $it" } ?: "Commit: -",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(modifier = Modifier.height(6.dp)) // 6.dp intentional: tight build meta gap
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
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    buildInfo?.buildTimestampUtc?.let {
                        DateUtils.format(
                            it,
                            DateUtils.Format.READABLE_DATE_TIME,
                        )
                    } ?: "-",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (buildInfo != null && buildInfo.isDirty) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                Text(
                    "Working tree dirty",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.logError,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun RecentEventsCard(
    modifier: Modifier = Modifier,
    events: List<TelemetryEvent>,
    onViewEvent: (TelemetryEvent) -> Unit = {},
    onViewEvents: () -> Unit = {},
) {
    OutlinedCard(
        modifier = modifier,
    ) {
        Column() {
            Row(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimens.margin.xxl, vertical = MaterialTheme.dimens.margin.md).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent Events",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onViewEvents,
                    contentPadding = PaddingValues(0.dp),
                ) {

                    Text(
                        "VIEW ALL LOGS",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            val rendered = events.asReversed()
            if (rendered.isEmpty()) {
                EmptyState(
                    icon = Icons.Server,
                    title = "No Events",
                )
            } else {
                LazyColumn {
                    items(rendered) { event ->
                        TelemetryItem(
                            event, showProperties = true,
                            onClick = {
                                onViewEvent(event)
                            },
                        )
                        if (event != events.last())
                            HorizontalDivider(
                                modifier = Modifier.padding(top = MaterialTheme.dimens.margin.lg),
                                thickness = MaterialTheme.dimens.stroke.thin,
                                color = MaterialTheme.colorScheme.subtleSurfaceAlt,
                            )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkTrafficLogsCard(
    apiLogs: List<TraceEntry>,
    onLogClick: (TraceEntry) -> Unit,
    modifier: Modifier = Modifier,
    onTracesClick: () -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
    ) {
        Column() {
            Row(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimens.margin.xxl, vertical = MaterialTheme.dimens.margin.md).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent Traces",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(
                    onClick = onTracesClick,
                    contentPadding = PaddingValues(0.dp),
                ) {

                    Text(
                        "VIEW ALL LOGS",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            val rendered = apiLogs.asReversed()
            if (rendered.isEmpty()) {
                EmptyState(
                    icon = Icons.Server,
                    title = "No Network Traces",
                )
            } else {
                LazyColumn {
                    items(rendered) { log ->
                        TraceItem(call = log, onClick = { onLogClick(log) })
                        if (log != rendered.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.subtleSurfaceAlt,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center
        )
    }
}
