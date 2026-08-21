package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ClearCapturedDialog
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.FollowNewest
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.components.rememberViewedStateColors
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorsPanel(
    devToolsViewModel: DevToolsViewModel,
    onErrorClick: (Error) -> Unit,
    searchFocusTrigger: Long = 0L,
) {
    val errors by devToolsViewModel.filteredErrors.collectAsState()
    val totalCount by devToolsViewModel.errors.collectAsState()
    val query by devToolsViewModel.errorQuery.collectAsState()
    val lazyListState = rememberLazyListState()
    var showClearConfirmation by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) searchFocus.requestFocus()
    }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Errors",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = if (query.isBlank()) {
                    "${totalCount.size} errors"
                } else {
                    "${errors.size} of ${totalCount.size} errors"
                },
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
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.sm,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraSearchTextField(
                    query = query,
                    onQueryChange = devToolsViewModel::onErrorQueryChange,
                    onClear = { devToolsViewModel.onErrorQueryChange("") },
                    modifier = Modifier.weight(1f).focusRequester(searchFocus),
                )
            }
            AlohomoraHorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (totalCount.isEmpty()) {
                    EmptyState(
                        icon = Icons.AlertTriangle,
                        title = "No errors yet",
                        subtitle = "Uncaught exceptions appear here automatically. " +
                            "Caught ones show up when the app calls Alohomora.recordError.",
                        setup = "try {\n    // ...\n} catch (e: Exception) {\n    Alohomora.recordError(e)\n}",
                    )
                } else if (errors.isEmpty()) {
                    EmptyState(
                        icon = Icons.AlertTriangle,
                        title = "No matching errors",
                        subtitle = "No errors match the current filter.",
                    )
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        contentPadding = PaddingValues(
                            MaterialTheme.dimens.margin.md,
                        ),
                    ) {
                        items(errors, key = { error -> error.id }) { error ->
                            ErrorRow(
                                error = error,
                                onClick = {
                                    devToolsViewModel.markErrorViewed(error.id)
                                    onErrorClick(error)
                                },
                            )
                        }
                        fabClearanceItem()
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
        FollowNewest(lazyListState, errors.size)

        if (showClearConfirmation) {
            ClearCapturedDialog(
                title = "Clear all errors?",
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

@Composable
private fun ErrorRow(
    error: Error,
    onClick: () -> Unit,
) {
    val viewedColors = rememberViewedStateColors(
        isViewed = error.isViewed,
        unviewedContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.38f),
        unviewedTitleColor = MaterialTheme.colorScheme.error,
    )
    AlohomoraCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(containerColor = viewedColors.containerColor.value),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AlertTriangle,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    tint = MaterialTheme.colorScheme.error,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = error.exceptionTypeName(),
                        style = MaterialTheme.typography.titleSmall,
                        color = viewedColors.titleColor.value,
                    )
                    AlohomoraChip(
                        label = "FATAL",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = DateUtils.format(error.time, DateUtils.Format.HH_MM_SS_3MS),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                error.place?.takeIf { it.isNotBlank() }?.let { place ->
                    Text(
                        text = place,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.xs),
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))

                error.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
