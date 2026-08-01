package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.Icon
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.TelemetryItem
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import androidx.compose.foundation.layout.Box
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsPanel(devToolsViewModel: DevToolsViewModel) {
    val events by devToolsViewModel.events.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var showProperties by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Telemetry",
                subtitle = "Live telemetry events from connected app",
                showDivider = lazyListState.canScrollBackward,
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
                            onCheckedChange = { showProperties = it },
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
    ) {
        Box(modifier = Modifier.padding(it).fillMaxSize()) {
            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                items(events, key = { event -> event.id to event.time }) { event ->
                    TelemetryItem(
                        event = event,
                        showProperties = showProperties,
                        onClick = { devToolsViewModel.markEventViewed(event.id) },
                    )
                    AlohomoraHorizontalDivider()
                }
            }
            ScrollToTopButton(lazyListState)
        }
        // Follows only while the user is already at the top; see FollowNewest.
        FollowNewest(lazyListState, events.size)

        if (showClearConfirmation) {
            ClearCapturedDialog(
                title = "Clear all events?",
                // Says "device" deliberately: this deletes at the source, not just this window.
                message = "Telemetry events will be deleted from the device. This cannot be undone.",
                onConfirm = {
                    devToolsViewModel.clearEvents()
                    showClearConfirmation = false
                },
                onDismiss = { showClearConfirmation = false },
            )
        }
    }
}
