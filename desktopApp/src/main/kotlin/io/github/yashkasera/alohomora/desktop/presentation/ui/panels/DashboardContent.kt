package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.model.DashboardUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EventItem
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TrafficItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCircularProgressIndicator
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Link
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Below this content width the two list cards stack instead of sitting side by side.
 *
 * Measured on the content box, not the window: the sidebar takes `fillMaxWidth(0.2f)`, and the
 * window opens maximised with no minimum size, so narrow really is reachable.
 */
private val STACK_BELOW = 720.dp

/** Height each list card takes when stacked, since it can no longer fill the row. */
private val STACKED_LIST_HEIGHT = 320.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardContent(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    selectedDevice: DeviceUi?,
    isRecording: Boolean,
    onTakeScreenshot: () -> Unit,
    onRecordScreen: () -> Unit,
    onTrafficItemClick: (TrafficEntry) -> Unit,
    onEventViewClick: (Event) -> Unit,
    onTrafficClick: () -> Unit,
    onEventsClick: () -> Unit,
    onOpenDeepLinkBuilder: () -> Unit,
) {
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val events by devToolsViewModel.events.collectAsState()
    val traffic by devToolsViewModel.traffic.collectAsState()
    val dashboard by devicesViewModel.dashboardState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        // Was the only panel without a top bar, which is most of why it read as a different
        // app. Device identity lives here now rather than in a card duplicating the sidebar.
        topBar = {
            AlohomoraTopBar(
                title = "Dashboard",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = buildString {
                    append(selectedDevice?.model ?: selectedDevice?.id ?: "No device")
                    if (dashboard.androidVersion != "-") {
                        append(" · Android ${dashboard.androidVersion} (API ${dashboard.apiLevel})")
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = MaterialTheme.dimens.margin.sm),
                    ) {
                        AlohomoraOutlinedButton(
                            text = "Screenshot",
                            onClick = onTakeScreenshot,
                            size = AlohomoraButtonSize.SMALL,
                        )
                        // Filled and red while recording: an outlined button reading "Stop"
                        // does not communicate that something is currently running.
                        if (isRecording) {
                            AlohomoraFilledButton(
                                text = "Stop Recording",
                                onClick = onRecordScreen,
                                size = AlohomoraButtonSize.SMALL,
                                containerColor = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            AlohomoraOutlinedButton(
                                text = "Record",
                                onClick = onRecordScreen,
                                size = AlohomoraButtonSize.SMALL,
                            )
                        }
                        AlohomoraOutlinedButton(
                            text = "Deep Link",
                            onClick = onOpenDeepLinkBuilder,
                            size = AlohomoraButtonSize.SMALL,
                            leadingIcon = {
                                Icon(
                                    Icons.Link,
                                    contentDescription = null,
                                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                                )
                            },
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = devicesViewModel.snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { insets ->
        BoxWithConstraints(modifier = Modifier.padding(insets).fillMaxSize()) {
            val stacked = maxWidth < STACK_BELOW

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Only scrolls when stacked; side by side the lists own their own scroll.
                    .then(if (stacked) Modifier.verticalScroll(scrollState) else Modifier)
                    .padding(MaterialTheme.dimens.margin.xxl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
            ) {
                MetricsStrip(dashboard)

                CurrentBuildCard(buildInfo = buildInfo)

                val eventsCard: @Composable (Modifier) -> Unit = { modifier ->
                    DashboardListCard(
                        title = "Recent Events",
                        onAction = onEventsClick,
                        isEmpty = events.isEmpty(),
                        icon = Icons.ChartLine,
                        emptyTitle = "No events yet",
                        itemCount = events.size,
                        modifier = modifier,
                    ) {
                        items(events, key = { event -> event.id to event.time }) { event ->
                            EventItem(
                                event = event,
                                // False here, true on the Events panel. A JSON block wraps
                                // badly in a half-width column, and the panel owns the detail.
                                showProperties = false,
                                onClick = { onEventViewClick(event) },
                            )
                            AlohomoraHorizontalDivider(
                                thickness = MaterialTheme.dimens.stroke.thin,
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            )
                        }
                    }
                }

                val trafficCard: @Composable (Modifier) -> Unit = { modifier ->
                    DashboardListCard(
                        title = "Recent Traffic",
                        onAction = onTrafficClick,
                        isEmpty = traffic.isEmpty(),
                        icon = Icons.Server,
                        emptyTitle = "No traffic yet",
                        itemCount = traffic.size,
                        modifier = modifier,
                    ) {
                        itemsIndexed(traffic, key = { _, log -> log.id }) { index, log ->
                            TrafficItem(call = log, onClick = { onTrafficItemClick(log) })

                        }
                    }
                }

                if (stacked) {
                    eventsCard(Modifier.fillMaxWidth().height(STACKED_LIST_HEIGHT))
                    trafficCard(Modifier.fillMaxWidth().height(STACKED_LIST_HEIGHT))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
                    ) {
                        eventsCard(Modifier.weight(2f).fillMaxSize())
                        trafficCard(Modifier.weight(2f).fillMaxSize())
                    }
                }
            }
        }
    }
}

/**
 * Device metrics, wrapping to as many rows as the width allows.
 *
 * CPU, FPS, frame time and jank were already being polled every 3 seconds by
 * [DevicesViewModel.startDashboardPolling] and thrown away — the Dashboard rendered only
 * battery, memory and latency. They are the only performance signal in the tool.
 *
 * `networkMbPerSec` is deliberately absent: it is hardcoded to "0.0" at the source, so showing
 * it would be a fabricated number rather than a missing one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MetricsStrip(dashboard: DashboardUiState) {
    // Only while genuinely empty. Gating on `loadingMetrics` alone would flash the whole strip
    // away every 3 seconds as each poll cycle begins.
    if (dashboard.loadingMetrics && dashboard.batteryPercent == "-") {
        AlohomoraOutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraCircularProgressIndicator()
            }
        }
        return
    }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
    ) {
        MetricTile("BATTERY", dashboard.batteryPercent, dashboard.batteryStatus, accentColor = MaterialTheme.alohomoraColors.success)
        MetricTile("MEMORY", dashboard.memoryUsageGb, "/ ${dashboard.memoryTotalGb} GB", accentColor = MaterialTheme.alohomoraColors.info)
        MetricTile("CPU", dashboard.cpuUsagePercent, accentColor = MaterialTheme.alohomoraColors.accent)
        MetricTile("FPS", dashboard.frameRateFps, "${dashboard.frameTimeMs}ms", accentColor = MaterialTheme.colorScheme.primary)
        MetricTile("JANK", dashboard.jankFrames, "frames", accentColor = MaterialTheme.alohomoraColors.warning)
        MetricTile(
            "LATENCY",
            "${dashboard.latencyMs}ms",
            valueColor = MaterialTheme.alohomoraColors.accent,
            accentColor = MaterialTheme.alohomoraColors.accent,
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    subValue: String? = null,
    valueColor: Color = Color.Unspecified,
    accentColor: Color = Color.Unspecified,
) {
    val resolvedValueColor =
        if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor
    val resolvedAccent =
        if (accentColor == Color.Unspecified) MaterialTheme.colorScheme.primary else accentColor
    AlohomoraOutlinedCard(modifier = Modifier.widthIn(min = 140.dp)) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.lg)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimens.icon.xs)
                        .clip(CircleShape)
                        .background(resolvedAccent),
                )
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    color = resolvedValueColor,
                )
                if (subValue != null) {
                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.xs))
                    Text(
                        subValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CurrentBuildCard(buildInfo: BuildInfo?) {
    AlohomoraOutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MaterialTheme.dimens.margin.xs)
                .background(MaterialTheme.colorScheme.primary),
        )
        Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    buildInfo?.versionName?.ifBlank { "-" } ?: "-",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                Text(
                    buildInfo?.versionCode?.toString()?.let { "($it)" } ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (buildInfo?.isDirty == true) {
                    // A chip rather than loose red text: it is a state of the build, and it
                    // reads as one at a glance.
                    AlohomoraChip(
                        label = "Dirty",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Text(
                buildInfo?.appName ?: "No build info",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            // FlowRow, not two SpaceBetween rows. SpaceBetween threw branch and commit to
            // opposite edges of a wide card, which is what made this look scattered.
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                BuildMeta("Branch", buildInfo?.branch ?: "-")
                BuildMeta("Commit", buildInfo?.commitSha?.take(7) ?: "-")
                BuildMeta(
                    "Variant",
                    listOfNotNull(
                        buildInfo?.variantName?.takeIf { it.isNotBlank() },
                        buildInfo?.buildType?.takeIf { it.isNotBlank() },
                        buildInfo?.flavorName?.takeIf { it.isNotBlank() },
                    ).joinToString(" • ").ifBlank { "-" },
                )
                BuildMeta(
                    "Built",
                    buildInfo?.buildTimestampUtc
                        ?.let { DateUtils.format(it, DateUtils.Format.READABLE_DATE_TIME) }
                        ?: "-",
                )
            }
        }
    }
}

@Composable
private fun BuildMeta(label: String, value: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.sm,
                vertical = MaterialTheme.dimens.margin.xs,
            ),
        ) {
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(value, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/**
 * A titled card wrapping a live list, with a "view all" action and an empty state.
 *
 * Extracted because the events and traffic cards were the same twenty lines twice — and had
 * drifted apart in the process: the events card used `if (event != events.last())` for its
 * dividers, an O(n) scan per row that compared by data equality rather than position, so two
 * identical events collapsed the divider between them.
 */
@Composable
private fun DashboardListCard(
    title: String,
    onAction: () -> Unit,
    isEmpty: Boolean,
    icon: ImageVector,
    emptyTitle: String,
    itemCount: Int,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    AlohomoraOutlinedCard(modifier = modifier) {
        Column {
            Row(
                modifier = Modifier
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.lg,
                    )
                    .padding(top = MaterialTheme.dimens.margin.md)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (itemCount > 0) {
                        AlohomoraChip(label = itemCount.toString())
                    }
                }
                AlohomoraTextButton(
                    text = "View All",
                    onClick = onAction,
                )
            }

            if (isEmpty) {
                EmptyState(icon = icon, title = emptyTitle)
            } else {
                val listState = rememberLazyListState()
                FollowNewest(listState, itemCount)
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    contentPadding = PaddingValues(
                        MaterialTheme.dimens.margin.md,
                    ),
                    content = content,
                )
            }
        }
    }
}
