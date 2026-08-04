package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.prettyProperties
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.KeyValueRow
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SectionLabel
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SlackShareDialog
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.EventsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Narrower than the traffic sheet: three key-value rows and one JSON block, no tabs.
 */
private const val EVENT_SHEET_WIDTH_FRACTION = 0.4f

/**
 * The whole payload for one event, which the list row deliberately only shows the head of.
 *
 * Reads the selection from [EventsViewModel] rather than taking an [Event] parameter the way
 * `TrafficDetailsSideSheet` does. The view model holds an id and re-resolves from the store, so this
 * cannot render a stale `isViewed` after [EventsViewModel.openEvent] marks it, nor survive a clear.
 */
@Composable
fun EventDetailsSideSheet(
    eventsViewModel: EventsViewModel,
    devToolsViewModel: DevToolsViewModel,
    onDismiss: () -> Unit,
) {
    val event by eventsViewModel.selectedEvent.collectAsState()
    val uiState by eventsViewModel.uiState.collectAsState()
    val slackShareError by devToolsViewModel.slackShareError.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val isSlackConfigured = buildInfo?.slackWebhookUrl.isNullOrBlank().not()
    val clipboardManager = LocalClipboardManager.current

    // Keyed on the event, not bare `remember`: this composable is reused as the selection changes, so
    // an un-keyed flag stays true across the switch and the dialog reopens for whichever event was
    // clicked next. Same trap TrafficDetailsSideSheet documents.
    var showSlackShareDialog by remember(event?.id) { mutableStateOf(false) }

    val properties = remember(event?.id, event?.time) { event?.prettyProperties() ?: "{}" }
    val shareText = remember(event?.id, properties) {
        // Mirrors the mobile sheet's clipboard text so both consoles paste the same thing.
        event?.let {
            buildString {
                appendLine("Event: ${it.name}")
                appendLine("Time: ${DateUtils.format(it.time, DateUtils.Format.ISO_DATE_TIME_SECONDS)}")
                appendLine()
                appendLine("Properties:")
                appendLine(properties)
            }
        }.orEmpty()
    }

    AlohomoraSideSheet(
        visible = event != null,
        onDismiss = onDismiss,
        widthFraction = EVENT_SHEET_WIDTH_FRACTION,
        header = {
            event?.let { selected ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.dimens.margin.xl,
                            vertical = MaterialTheme.dimens.margin.md,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selected.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = DateUtils.format(selected.time, DateUtils.Format.ISO_DATE_TIME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AlohomoraIconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(shareText)) },
                    ) {
                        Icon(imageVector = Icons.Copy, contentDescription = "Copy event")
                    }
                    // Hidden rather than disabled when unconfigured: an action that cannot work should
                    // not be offered, the same rule replay follows.
                    if (isSlackConfigured) {
                        AlohomoraIconButton(onClick = { showSlackShareDialog = true }) {
                            Icon(imageVector = Icons.Slack, contentDescription = "Share to Slack")
                        }
                    }
                    AlohomoraIconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.X, contentDescription = "Close")
                    }
                }
            }
        },
    ) {
        event?.let { selected ->
            EventDetailsContent(
                event = selected,
                properties = properties,
                isMuted = selected.name in uiState.filters.mutedNames,
                onToggleMute = { eventsViewModel.toggleMute(selected.name) },
                onSolo = { eventsViewModel.soloName(selected.name) },
            )
        }
    }

    event?.let { selected ->
        if (showSlackShareDialog) {
            SlackShareDialog(
                isConfigured = isSlackConfigured,
                currentWebhookUrl = buildInfo?.slackWebhookUrl,
                shareError = slackShareError,
                onDismiss = {
                    showSlackShareDialog = false
                    devToolsViewModel.clearSlackShareError()
                },
                // No cURL variant: an event has one representation.
                onShareText = { email ->
                    devToolsViewModel.shareEventToSlack(selected, email) {
                        showSlackShareDialog = false
                    }
                },
                shareTextLabel = "Share Event to Slack",
                onClearError = devToolsViewModel::clearSlackShareError,
            )
        }
    }
}

/**
 * The sheet's body, taking plain data so a test can compose the real tree.
 *
 * Split out of [EventDetailsSideSheet] because the view models it otherwise needs would cost a
 * thirty-member `DevToolsRepository` fake to reach — and this is the subtree where a layout bug lives:
 * it hosts the only vertically scrollable component in the sheet.
 *
 * Declared in `ColumnScope` on purpose. [AlohomoraSideSheet] invokes its content slot inside a `Column`,
 * and taking the scope here is what lets this claim a bounded share of the sheet's height with
 * [Modifier.weight] rather than asking for an unbounded one.
 */
@Composable
internal fun ColumnScope.EventDetailsContent(
    event: Event,
    properties: String,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onSolo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(MaterialTheme.dimens.margin.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        KeyValueRow(label = "Name", value = event.name, monospaceValue = true)
        KeyValueRow(
            label = "Time",
            value = DateUtils.format(event.time, DateUtils.Format.ISO_DATE_TIME),
            monospaceValue = true,
        )
        KeyValueRow(label = "Read", value = if (event.isViewed) "Yes" else "No")

        // Solo lives here rather than on the name chips: a FilterChip's trailing icon is not
        // independently clickable, so in the chip row it would have to be a right-click menu (a hidden
        // gesture) or a second chip per name. Here it is a labelled button, next to the name it acts on.
        Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
            AlohomoraOutlinedButton(
                text = if (isMuted) "Unmute this event" else "Mute this event",
                onClick = onToggleMute,
            )
            AlohomoraOutlinedButton(text = "Show only this", onClick = onSolo)
        }

        SectionLabel("Properties")
        // Scrollable here, unlike the list row: the sheet is the one place the whole payload is meant to
        // be readable, and it owns its scroll rather than competing with a list. Weighted so it takes
        // the height left over instead of asking for an infinite one.
        AlohomoraCodeBlock(
            content = properties,
            isScrollable = true,
            modifier = Modifier.weight(1f),
        )
    }
}
