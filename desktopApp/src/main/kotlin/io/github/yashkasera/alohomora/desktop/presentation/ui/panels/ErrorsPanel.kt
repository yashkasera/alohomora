package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorsPanel(devToolsViewModel: DevToolsViewModel) {
    val errors by devToolsViewModel.errors.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    var expandedId by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Errors",
                subtitle = "Crashes and reported failures from connected app",
                showDivider = lazyListState.canScrollBackward,
                actions = {
                    AlohomoraIconButton(onClick = { showClearConfirmation = true }) {
                        Icon(
                            imageVector = Icons.Trash,
                            contentDescription = "Clear all errors",
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Box(modifier = Modifier.padding(it).fillMaxSize()) {
            if (errors.isEmpty()) {
                EmptyState(
                    icon = Icons.AlertTriangle,
                    title = "No errors yet",
                    // Says what to do about it: unlike traffic or events, an empty Errors panel is
                    // the desired state, and the one thing worth checking is whether the app is
                    // new enough to report at all.
                    subtitle = "Uncaught exceptions appear here automatically. " +
                        "Caught ones show up when the app calls Alohomora.recordError.",
                )
            } else {
                LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                    items(errors, key = { error -> error.id }) { error ->
                        ErrorRow(
                            error = error,
                            isExpanded = expandedId == error.id,
                            onClick = {
                                expandedId = if (expandedId == error.id) null else error.id
                                devToolsViewModel.markErrorViewed(error.id)
                            },
                        )
                        AlohomoraHorizontalDivider()
                    }
                }
                ScrollToTopButton(lazyListState)
            }
        }
        // Follows only while the user is already at the top; see FollowNewest.
        FollowNewest(lazyListState, errors.size)

        if (showClearConfirmation) {
            ClearCapturedDialog(
                title = "Clear all errors?",
                // Says "device" deliberately: this deletes at the source, not just this window.
                message = "Errors will be deleted from the device. This cannot be undone.",
                onConfirm = {
                    devToolsViewModel.clearErrors()
                    showClearConfirmation = false
                },
                onDismiss = { showClearConfirmation = false },
            )
        }
    }
}

/**
 * One error, with its stack trace collapsed until clicked.
 *
 * Collapsed by default because a trace is dozens of lines: expanded rows would let two errors fill
 * the viewport and make the list useless for scanning, which is what it is mainly for.
 */
@Composable
private fun ErrorRow(
    error: Error,
    isExpanded: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = error.exceptionTypeName(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (error.isViewed) FontWeight.Normal else FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            AlohomoraChip(
                label = "FATAL",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
            Box(modifier = Modifier.weight(1f))
            Text(
                text = DateUtils.format(error.time, DateUtils.Format.HH_MM_SS_3MS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        error.reason?.takeIf { it.isNotBlank() }?.let { reason ->
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
            )
        }

        error.place?.takeIf { it.isNotBlank() }?.let { place ->
            Text(
                text = place,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
            )
        }

        if (isExpanded) {
            val trace = error.stackTrace?.takeIf { it.isNotBlank() }
                // Only reachable for an error reported through the string overload without one.
                ?: "No stack trace was recorded for this error."
            Box(
                modifier = Modifier
                    .padding(top = MaterialTheme.dimens.margin.md)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    // Traces are wide and must not wrap: a wrapped frame is far harder to read
                    // than one that scrolls.
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = trace,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
                )
            }
        }
    }
}
