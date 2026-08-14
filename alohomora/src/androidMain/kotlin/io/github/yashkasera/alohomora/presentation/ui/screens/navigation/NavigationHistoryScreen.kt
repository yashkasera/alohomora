package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun NavigationHistoryScreen(onBackClick: () -> Unit = {}) {
    val viewModel = koinViewModel<NavigationHistoryViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Navigation History",
                subtitle = null,
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.RefreshCw, contentDescription = "refresh")
                    }
                },
            )
        },
        bottomBar = { SessionSummaryBar(state) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
        ) {
            NavigationTimeline(events = state.timelineEvents)
        }
    }
}

@Composable
internal fun NavigationTimeline(events: List<ActivityTimelineItem>) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = MaterialTheme.dimens.margin.xxxl)
                .testTag("nav_empty"),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Route,
                title = "No navigation yet",
                subtitle = "Screens appear here as the app navigates.",
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth().testTag("nav_timeline")) {
        events.forEachIndexed { index, event ->
            TimelineItem(
                event = event,
                index = index,
                isFirst = index == 0,
                isLast = index == events.lastIndex,
            )
        }
    }
}

@Composable
private fun TimelineItem(
    event: ActivityTimelineItem,
    index: Int,
    isFirst: Boolean,
    isLast: Boolean,
) {
    var isIntentExpanded by remember { mutableStateOf(false) }

    val hasExtras = !event.intentExtras.isNullOrEmpty()
    val hasIntentData = !event.intentAction.isNullOrBlank() ||
        !event.intentData.isNullOrBlank() ||
        hasExtras

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .testTag("nav_timeline_item_$index"),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        TimelineRail(isActive = event.isActive, isFirst = isFirst, isLast = isLast)

        // Content card. Bottom padding on all but the last leaves the gap the rail bridges.
        AlohomoraCard(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) MaterialTheme.dimens.margin.xs else MaterialTheme.dimens.margin.md),
            colors = AlohomoraCardDefaults.colors(
                containerColor = if (event.isActive) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.dimens.margin.md),
            ) {
                // Status chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StateChip(label = event.stateLabel, isActive = event.isActive)
                    if (event.badge != null) {
                        AlohomoraChip(label = event.badge)
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                // Screen name
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("nav_screen_title"),
                )

                // Quick intent summary (deeplink / action)
                if (!event.intentData.isNullOrBlank() || !event.intentAction.isNullOrBlank()) {
                    Text(
                        text = event.intentData ?: event.intentAction.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                // Meta row: timestamp · duration on the left, intent toggle on the right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = buildString {
                            append(event.timestamp)
                            event.duration?.let { append(" · "); append(it) }
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (hasIntentData) {
                        Row(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { isIntentExpanded = !isIntentExpanded }
                                .padding(
                                    horizontal = MaterialTheme.dimens.margin.xs,
                                    vertical = MaterialTheme.dimens.margin.xs,
                                )
                                .testTag("nav_intent_toggle")
                                .semantics {
                                    contentDescription =
                                        if (isIntentExpanded) "expanded" else "collapsed"
                                },
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (hasExtras) "EXTRAS" else "INTENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = if (isIntentExpanded) Icons.ChevronDown else Icons.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            )
                        }
                    }
                }

                if (hasIntentData && isIntentExpanded) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    IntentDetails(event)
                }
            }
        }
    }
}

@Composable
private fun IntentDetails(event: ActivityTimelineItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = MaterialTheme.dimens.stroke.small,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(MaterialTheme.dimens.margin.md),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
    ) {
        event.intentAction?.takeIf { it.isNotBlank() }?.let { IntentRow("action", it) }
        event.intentData?.takeIf { it.isNotBlank() }?.let { IntentRow("data", it) }
        event.intentExtras?.forEach { (key, value) -> IntentRow(key, value) }
    }
}

@Composable
private fun IntentRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(0.6f),
        )
    }
}

/**
 * The left rail: a continuous vertical line with a dot at this item's header. The line is drawn as a
 * top segment (above the dot) and a bottom segment (below, filling the rest of the row). The top
 * segment is hidden on the first item and the bottom on the last, so the line runs unbroken *through*
 * every dot between cards but stops cleanly at the ends. The active dot is accent-filled to tie it to
 * the FOREGROUND chip.
 */
@Composable
private fun TimelineRail(
    isActive: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.alohomoraColors.accent
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(MaterialTheme.dimens.icon.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top segment: from row top down to the dot, aligned with the card's header padding.
        Box(
            modifier = Modifier
                .width(MaterialTheme.dimens.stroke.small)
                .height(MaterialTheme.dimens.margin.lg)
                .background(if (isFirst) Color.Transparent else lineColor),
        )

        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.margin.md)
                .semantics { contentDescription = if (isActive) "active screen" else "screen" }
                .border(
                    width = MaterialTheme.dimens.stroke.medium,
                    color = if (isActive) accent else lineColor,
                    shape = CircleShape,
                )
                .background(
                    color = if (isActive) accent else MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                ),
        )

        // Bottom segment: fills the remaining row height, bridging the gap to the next dot.
        Box(
            modifier = Modifier
                .width(MaterialTheme.dimens.stroke.small)
                .weight(1f)
                .background(if (isLast) Color.Transparent else lineColor),
        )
    }
}

@Composable
private fun StateChip(
    label: String,
    isActive: Boolean,
) {
    val accent = MaterialTheme.alohomoraColors.accent
    // AlohomoraChip exposes no modifier, so the testTag rides on a wrapping Box.
    Box(modifier = Modifier.testTag(if (isActive) "nav_active_badge" else "nav_state_label")) {
        AlohomoraChip(
            label = label,
            containerColor = if (isActive) accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isActive) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionSummaryBar(state: NavigationHistoryState) {
    Column {
        AlohomoraHorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xl,
                    vertical = MaterialTheme.dimens.margin.lg,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            SummaryStat(label = "SESSION DURATION", value = state.sessionDuration)
            SummaryStat(
                label = "SCREENS VISITED",
                value = state.screensVisited.toString(),
                alignEnd = true,
                valueTestTag = "nav_screens_visited",
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    alignEnd: Boolean = false,
    valueTestTag: String? = null,
) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = (if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier)
                .padding(top = MaterialTheme.dimens.margin.xs),
        )
    }
}
