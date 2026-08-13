package io.github.yashkasera.alohomora.presentation.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.presentation.ui.components.EventsDetailsSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Eye
import io.github.yashkasera.alohomora.ui.icons.EyeOff
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.serialization.json.JsonNull
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventsScreen(onBackClick: () -> Unit) {
    val viewModel = koinViewModel<EventsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isSlackConfigured = remember { Alohomora.config?.slackWebhookUrl.isNullOrBlank().not() }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Events",
                subtitle = if (state.events.isEmpty()) null else "${state.events.size} EVENTS",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(onClick = viewModel::toggleShowProperties) {
                        Icon(
                            imageVector = if (state.showProperties) Icons.Eye else Icons.EyeOff,
                            contentDescription = if (state.showProperties) "Hide properties" else "Show properties",
                        )
                    }
                    if (state.events.isNotEmpty()) {
                        AlohomoraIconButton(onClick = viewModel::showClearConfirmation) {
                            Icon(imageVector = Icons.Trash, contentDescription = "Clear all events")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            EventsSearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )
            EventsList(
                events = state.events,
                showProperties = state.showProperties,
                onEventClick = viewModel::onEventClick,
            )
        }

        state.selectedEvent?.let { event ->
            EventsDetailsSheet(
                event = event,
                isSlackConfigured = isSlackConfigured,
                onDismiss = viewModel::dismissEventDetail,
                onShareToSlack = { viewModel.hideSlackSheet() },
            )
        }

        if (state.showClearConfirmation) {
            ConfirmationBottomSheet(
                config = ConfirmationConfig(
                    title = "Clear All Events",
                    message = "Are you sure you want to delete all telemetry events? This action cannot be undone.",
                    confirmButtonText = "Clear All",
                    dismissButtonText = "Cancel",
                    isDestructive = true,
                ),
                onConfirm = viewModel::clearAllEvents,
                onDismiss = viewModel::hideClearConfirmation,
            )
        }
    }
}

@Composable
private fun EventsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    AlohomoraSearchTextField(
        query = query,
        onQueryChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.dimens.margin.md),
        placeholder = "Search events by name",
    )
}

@Composable
internal fun EventsList(
    events: List<Event>,
    showProperties: Boolean,
    onEventClick: (Event) -> Unit,
) {
    if (events.isEmpty()) {
        EmptyState(
            icon = Icons.ChartLine,
            title = "No Events Yet",
            subtitle = "Events will appear here in real-time",
        )
    } else {
        val listState = rememberLazyListState()
        FollowNewest(listState, events.size)
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                items(events, key = { it.id }) { event ->
                    EventItem(
                        event = event,
                        showProperties = showProperties,
                        onClick = { onEventClick(event) },
                    )
                }
            }
            ScrollToTopButton(listState)
        }
    }
}

@Composable
internal fun EventItem(
    event: Event,
    showProperties: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = when {
        event.isViewed -> MaterialTheme.colorScheme.surfaceContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Text(
                    text = DateUtils.format(event.time, DateUtils.Format.HH_MM_SS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (showProperties) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                Text(
                    text = event.properties
                        ?.takeUnless { it is JsonNull }
                        ?.toString()
                        ?: "{}",
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = MaterialTheme.dimens.margin.xs)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            width = MaterialTheme.dimens.stroke.small,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(MaterialTheme.dimens.margin.sm),
                )
            }
        }
    }
}
