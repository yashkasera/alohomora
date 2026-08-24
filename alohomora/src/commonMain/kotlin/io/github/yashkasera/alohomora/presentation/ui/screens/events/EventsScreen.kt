package io.github.yashkasera.alohomora.presentation.ui.screens.events

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.clampLines
import io.github.yashkasera.alohomora.common.isCrashEvent
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.presentation.ui.components.EventsDetailsSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.ConfirmationBottomSheet
import io.github.yashkasera.alohomora.ui.components.ConfirmationConfig
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.components.rememberViewedStateColors
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Eye
import io.github.yashkasera.alohomora.ui.icons.EyeOff
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EventsScreen(onBackClick: () -> Unit) {
    val viewModel = koinViewModel<EventsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isSlackConfigured = remember { AlohomoraImpl.config?.slackWebhookUrl.isNullOrBlank().not() }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Events",
                subtitle = if (state.events.isEmpty()) null else "${state.events.size} EVENTS",
                navigationIcon = {
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(
                        onClick = viewModel::toggleShowProperties,
                        modifier = Modifier.testTag(AlohomoraTestTags.Events.PROPERTIES_TOGGLE),
                    ) {
                        Icon(
                            imageVector = if (state.showProperties) Icons.Eye else Icons.EyeOff,
                            contentDescription = if (state.showProperties) "Hide properties" else "Show properties",
                        )
                    }
                    if (state.events.isNotEmpty()) {
                        AlohomoraIconButton(
                            onClick = viewModel::showClearConfirmation,
                            modifier = Modifier.testTag(AlohomoraTestTags.Chrome.CLEAR_ALL),
                        ) {
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
                modifier = Modifier.testTag(AlohomoraTestTags.Events.DETAILS_SHEET),
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
            .padding(MaterialTheme.dimens.margin.md)
            .testTag(AlohomoraTestTags.Chrome.SEARCH),
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
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(AlohomoraTestTags.Events.LIST),
                contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                items(events, key = { it.id }) { event ->
                    EventItem(
                        event = event,
                        showProperties = showProperties,
                        onClick = { onEventClick(event) },
                        modifier = Modifier.testTag(AlohomoraTestTags.Events.item(event.id)),
                    )
                }
                fabClearanceItem()
            }
            ScrollToTopButton(listState)
        }
    }
}

private const val MAX_ROW_PROPERTY_LINES = 5

@Composable
internal fun EventItem(
    event: Event,
    showProperties: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFatal = event.isCrashEvent
    val viewedColors = rememberViewedStateColors(
        isViewed = event.isViewed,
        unviewedTitleColor = if (isFatal) MaterialTheme.alohomoraColors.fatal
        else MaterialTheme.colorScheme.onSurface,
        viewedTitleColor = if (isFatal) MaterialTheme.alohomoraColors.fatal
        else MaterialTheme.colorScheme.onSurfaceVariant,
    )
    AlohomoraCard(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = viewedColors.containerColor.value,
        ),
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            if (isFatal) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(MaterialTheme.dimens.stroke.medium)
                        .background(MaterialTheme.alohomoraColors.fatal),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(MaterialTheme.dimens.margin.md),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = viewedColors.titleColor.value,
                    )
                    Text(
                        text = DateUtils.format(event.time, DateUtils.Format.HH_MM_SS),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showProperties) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    val properties = remember(event.id, event.time) {
                        event.prettyProperties().clampLines(MAX_ROW_PROPERTY_LINES)
                    }
                    AlohomoraCodeBlock(
                        content = properties,
                        isScrollable = false,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun EventItemNormalPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventItem(
                event = Event(
                    id = 1,
                    name = "screen_view",
                    properties = buildJsonObject { put("screen", JsonPrimitive("HomeScreen")) },
                    time = 1724234567000L,
                    isViewed = false,
                ),
                showProperties = false,
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Preview
@Composable
private fun EventItemFatalPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventItem(
                event = Event(
                    id = 2,
                    name = "App.Exception",
                    properties = buildJsonObject {
                        put("reason", JsonPrimitive("NullPointerException"))
                    },
                    time = 1724234567000L,
                    isViewed = false,
                ),
                showProperties = false,
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Preview
@Composable
private fun EventItemWithPropertiesPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventItem(
                event = Event(
                    id = 3,
                    name = "button_click",
                    properties = buildJsonObject {
                        put("button_id", JsonPrimitive("checkout"))
                        put("screen", JsonPrimitive("CartScreen"))
                        put("item_count", JsonPrimitive(3))
                    },
                    time = 1724234567000L,
                    isViewed = true,
                ),
                showProperties = true,
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}

@Preview
@Composable
private fun EventItemViewedPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            EventItem(
                event = Event(
                    id = 4,
                    name = "api_response",
                    properties = null,
                    time = 1724234567000L,
                    isViewed = true,
                ),
                showProperties = false,
                onClick = {},
                modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
            )
        }
    }
}
