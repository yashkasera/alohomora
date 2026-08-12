package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.eventsSubtitle
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EventItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.EyeOff
import io.github.yashkasera.alohomora.ui.icons.Filter
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsPanel(eventsViewModel: EventsViewModel) {
    val state by eventsViewModel.uiState.collectAsState()
    val showProperties by eventsViewModel.showProperties.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Events",
                // Says how much the filters removed, and names the store cap when it bites — a total
                // stalled at exactly the cap otherwise looks like a stalled stream.
                subtitle = eventsSubtitle(state),
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Show Properties",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge,
                        )

                        Switch(
                            checked = showProperties,
                            onCheckedChange = { eventsViewModel.toggleShowProperties() },
                        )

                        AlohomoraIconButton(onClick = { showClearConfirmation = true }) {
                            Icon(
                                imageVector = Icons.Trash,
                                contentDescription = "Clear all events",
                            )
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            EventsFilters(
                state = state,
                onQueryChange = eventsViewModel::onQueryChange,
                onUnreadOnlyChange = eventsViewModel::onUnreadOnlyChange,
                onWindowChange = eventsViewModel::onWindowChange,
                onMark = eventsViewModel::mark,
                onClearMark = eventsViewModel::clearMark,
                onToggleMute = eventsViewModel::toggleMute,
                onUnmuteAll = eventsViewModel::unmuteAll,
                onClearFilters = eventsViewModel::clearFilters,
            )
            AlohomoraHorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.events.isEmpty()) {
                    EventsEmptyState(
                        state = state,
                        onUnmuteAll = eventsViewModel::unmuteAll,
                        onClearFilters = eventsViewModel::clearFilters,
                    )
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        contentPadding = PaddingValues(
                            MaterialTheme.dimens.margin.md
                        )
                    ) {
                        items(state.events, key = { event -> event.id to event.time }) { event ->
                            EventItem(
                                event = event,
                                showProperties = showProperties,
                                onClick = { eventsViewModel.openEvent(event.id) },
                            )
                        }
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
        // Follows only while the user is already at the top; see FollowNewest.
        FollowNewest(lazyListState, state.events.size)

        if (showClearConfirmation) {
            ClearCapturedDialog(
                title = "Clear all events?",
                // Says "device" deliberately: this deletes at the source, not just this window. The
                // "Hide older" filter is the non-destructive alternative and must not read like this.
                message = "Events will be deleted from the device. This cannot be undone.",
                onConfirm = {
                    eventsViewModel.clearEvents()
                    showClearConfirmation = false
                },
                onDismiss = { showClearConfirmation = false },
            )
        }
    }
}

/**
 * Distinguishes the three reasons this panel can be empty.
 *
 * Worth the branching for the same reason `TracesEmptyState` earns its own: each case needs a different
 * action from the reader. The muted case is called out separately because mutes are the one filter that
 * survives a restart — a generic "no events match" against a set muted days ago reads as a dead stream,
 * which is exactly the failure that keeps every other filter out of `Preferences`.
 */
@Composable
internal fun EventsEmptyState(
    state: EventsUiState,
    onUnmuteAll: () -> Unit,
    onClearFilters: () -> Unit,
) {
    when {
        state.totalCount == 0 -> EmptyState(
            icon = Icons.ChartLine,
            title = "No events yet",
            subtitle = "Events appear here as the connected app records them.",
        )

        !state.filters.hasTransientFilter -> EmptyState(
            icon = Icons.EyeOff,
            title = "Every event is muted",
            subtitle = "${state.filters.mutedNames.size} names are muted for this device, " +
                "hiding all ${state.totalCount} captured events.",
            action = { AlohomoraOutlinedButton(text = "Unmute all", onClick = onUnmuteAll) },
        )

        else -> EmptyState(
            icon = Icons.Filter,
            title = "No events match",
            subtitle = "${state.totalCount} captured. Clear the filters to see them.",
            action = { AlohomoraOutlinedButton(text = "Clear filters", onClick = onClearFilters) },
        )
    }
}
