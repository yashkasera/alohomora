package io.github.yashkasera.alohomora.presentation.ui.screens.error.list

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ErrorScreen(
    onBackClick: () -> Unit,
    onNavigateToError: (errorId: Long) -> Unit,
) {
    val viewModel = koinViewModel<ErrorViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Errors",
                subtitle = if (state.errors.isEmpty()) null else "${state.errors.size} OCCURRENCES",
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
                        onClick = { viewModel.clearAllErrors() },
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.CLEAR_ALL),
                    ) {
                        Icon(
                            Icons.Trash,
                            contentDescription = "Clear all errors",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                // No in-content title: the top bar already says "Errors". This screen was
                // the only one repeating its own name, which cost a third of the viewport.
                AlohomoraSearchTextField(
                    query = state.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChange(it) },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimens.margin.md)
                        .testTag(AlohomoraTestTags.Chrome.SEARCH),
                    placeholder = "Search exceptions or packages...",
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                Row(
                    modifier = Modifier.fillMaxWidth()

                        .padding(horizontal = MaterialTheme.dimens.margin.md),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${state.errors.size} TOTAL OCCURRENCES",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.errors.isEmpty()) {
                EmptyState(
                    icon = Icons.AlertTriangle,
                    title = "No Errors Recorded",
                    subtitle = "Error reports will appear here when exceptions occur",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                val listState = rememberLazyListState()
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(AlohomoraTestTags.Errors.LIST),
                        contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    ) {
                        items(state.errors, key = { it.id }) { error ->
                            ErrorListItem(
                                error = error,
                                onClick = { onNavigateToError(error.id) },
                                modifier = Modifier.testTag(AlohomoraTestTags.Errors.item(error.id)),
                            )
                        }
                        fabClearanceItem()
                    }
                    ScrollToTopButton(listState)
                }
            }
        }
    }
}

@Composable
private fun ErrorListItem(
    error: Error,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlohomoraCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.md),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = error.exceptionTypeName(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                        AlohomoraChip(
                            label = "FATAL",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(Modifier.height(MaterialTheme.dimens.margin.xs))

                    Text(
                        text = error.place ?: "Unknown location",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )

                    Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))

                    Text(
                        text = DateUtils.format(error.time, DateUtils.Format.READABLE_DATE_TIME),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
