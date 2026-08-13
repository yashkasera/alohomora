package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import io.github.yashkasera.alohomora.ui.icons.ArrowRight
import io.github.yashkasera.alohomora.ui.theme.dimens
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
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
                        Icon(Icons.RefreshCw, contentDescription = null)
                    }
                },
            )
        },
        bottomBar = {
            // Session Summary
            AlohomoraHorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = MaterialTheme.dimens.margin.xl, vertical = MaterialTheme.dimens.margin.xl),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "SESSION DURATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.sessionDuration,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "STACK OPERATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        state.stackOperations.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.margin.xl),
        ) {
            // Timeline
            NavigationTimeline(events = state.timelineEvents)
        }
    }
}

@Composable
private fun NavigationTimeline(events: List<ActivityTimelineItem>) {
    if (events.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = MaterialTheme.dimens.margin.xs),
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

    Column(modifier = Modifier.fillMaxWidth()) {
        events.forEachIndexed { index, event ->
            TimelineItem(event = event)

            // Add connector between items (except after last item)
            if (index < events.lastIndex) {
                TimelineConnector(
                    hasArrow = false,
                    label = null,
                )
            }
        }
    }
}

@Composable
private fun TimelineItem(
    event: ActivityTimelineItem,
) {
    var isIntentExpanded by remember { mutableStateOf(false) }

    val hasIntentData = !event.intentAction.isNullOrBlank() ||
        !event.intentData.isNullOrBlank() ||
        !event.intentExtras.isNullOrEmpty()
    val hasExtras = !event.intentExtras.isNullOrEmpty()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
    ) {
        // Timeline dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(MaterialTheme.dimens.margin.xl),
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.margin.xl)
                    .border(
                        width = MaterialTheme.dimens.stroke.medium,
                        color = if (event.isActive) MaterialTheme.colorScheme.onBackground
                        else MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    )
                    .background(
                        color = if (event.isActive) MaterialTheme.colorScheme.onBackground
                        else Color.Transparent,
                        shape = CircleShape,
                    ),
            )
        }

        // Content Card
        Box(
            modifier = Modifier
                .weight(1f)
                .border(
                    width = if (event.isActive) MaterialTheme.dimens.stroke.medium else MaterialTheme.dimens.stroke.small,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RectangleShape,
                )
                .background(
                    color = if (event.isActive) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                )
                .padding(MaterialTheme.dimens.margin.xl),
        ) {
            Column {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        // State label (RESUMED, POPPED SCREEN, etc)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (event.isActive) "RESUMED" else when {
                                    event.intentData != null -> "POPPED SCREEN"
                                    hasExtras -> "PREVIOUS"
                                    else -> "ENTRY POINT"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            if (event.badge != null && event.badge != "CURRENT") {
                                Box(
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.extraSmall)
                                        .background(MaterialTheme.colorScheme.onBackground)
                                        .padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = MaterialTheme.dimens.margin.xs),
                                ) {
                                    Text(
                                        text = event.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.background,
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (event.isActive) {
                            Box(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(MaterialTheme.colorScheme.onBackground)
                                    .padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = MaterialTheme.dimens.margin.xs),
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.background,
                                )
                            }
                        }

                        // Show arrow for PREVIOUS state
                        if (!event.isActive && hasExtras && event.intentData == null) {
                            Icon(
                                imageVector = Icons.ArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                // Screen name
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                // Intent Data Section (expandable)
                if (hasIntentData) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isIntentExpanded = !isIntentExpanded }
                            .padding(vertical = MaterialTheme.dimens.margin.xs),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (hasExtras) "EXTRAS" else "INTENT DATA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (isIntentExpanded) "∧" else "∨",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isIntentExpanded) {
                        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                        // Show intent extras
                        event.intentExtras?.forEach { (key, value) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "$key:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = value,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        // Show deeplink if available
                        if (event.intentData != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "deepLink:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = event.intentData,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                // Metadata row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "TIMESTAMP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = event.timestamp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
                        )
                    }

                    if (event.duration != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (event.isActive) "RESUME DELAY" else "DURATION",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = event.duration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineConnector(
    hasArrow: Boolean = false,
    label: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.margin.sm),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
    ) {
        // Vertical line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(MaterialTheme.dimens.margin.xl),
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
        }

        // Arrow and label
        if (hasArrow && label != null) {
            Row(
                modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.ArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
