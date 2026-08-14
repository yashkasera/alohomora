package io.github.yashkasera.alohomora.presentation.ui.screens.featureflags

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.ToggleLeft
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FeatureFlagsScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: FeatureFlagsViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Feature Flags",
                subtitle = "flags & config",
                navigationIcon = {
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
                        Icon(Icons.ArrowLeft, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            FeatureFlagsFooter(
                modifier = Modifier
                    .padding(MaterialTheme.dimens.margin.lg)
                    .testTag(AlohomoraTestTags.FeatureFlags.FOOTER),
                totalCount = state.totalCount,
                filteredCount = state.filteredCount,
                sourceCount = state.sources.size,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AlohomoraSearchTextField(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = MaterialTheme.dimens.margin.md)
                    .testTag(AlohomoraTestTags.Chrome.SEARCH),
                placeholder = "Filter flags...",
            )

            if (state.sources.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AlohomoraTestTags.FeatureFlags.SOURCES),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    contentPadding = PaddingValues(horizontal = MaterialTheme.dimens.margin.md),
                ) {
                    items(state.sources) { source ->
                        AlohomoraFilterChip(
                            label = source,
                            selected = state.selectedSource == source,
                            onClick = { viewModel.onSourceSelected(source) },
                            modifier = Modifier.testTag(
                                AlohomoraTestTags.FeatureFlags.sourceFilter(source),
                            ),
                        )
                    }
                }
            }

            if (state.flags.isEmpty()) {
                EmptyState(
                    icon = Icons.ToggleLeft,
                    title = "No Feature Flags",
                    subtitle = "Use Alohomora.recordFeatureFlag() to push flags",
                )
            } else if (state.filteredFlags.isEmpty()) {
                EmptyState(
                    icon = Icons.Search,
                    title = "No Results",
                    subtitle = "Try adjusting your search query",
                )
            } else {
                FeatureFlagsList(flags = state.filteredFlags)
            }
        }
    }
}

@Composable
private fun FeatureFlagsList(flags: List<FeatureFlag>) {
    val listState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AlohomoraTestTags.FeatureFlags.LIST),
            contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            items(
                items = flags,
                key = { it.key },
            ) { flag ->
                FeatureFlagItem(
                    flag = flag,
                    modifier = Modifier.testTag(AlohomoraTestTags.FeatureFlags.item(flag.key)),
                )
            }
            fabClearanceItem()
        }
        ScrollToTopButton(listState)
    }
}

@Composable
private fun FeatureFlagItem(
    flag: FeatureFlag,
    modifier: Modifier = Modifier,
) {
    AlohomoraCard(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
        ) {
            Text(
                text = flag.key,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )


            val valueColor = when {
                flag.value.equals("true", ignoreCase = true) -> MaterialTheme.colorScheme.primary
                flag.value.equals("false", ignoreCase = true) -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }

            Text(
                modifier = Modifier.fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(MaterialTheme.dimens.margin.sm),
                text = flag.value,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )

            val hasChips = flag.type != null || flag.source != null
            val meta = flag.metadata
            if (!meta.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                Text(
                    text = meta.entries.joinToString(" | ") { "${it.key}: ${it.value}" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (hasChips) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    flag.type?.let { type -> AlohomoraChip(label = type) }
                    flag.source?.let { source -> AlohomoraChip(label = source) }
                }
            }

        }
    }
}

@Composable
private fun FeatureFlagsFooter(
    modifier: Modifier = Modifier,
    totalCount: Int,
    filteredCount: Int,
    sourceCount: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .then(modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "FEATURE FLAGS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val text = buildString {
                if (filteredCount != totalCount) {
                    append("$filteredCount of $totalCount flags")
                } else {
                    append("$totalCount flags")
                }
                if (sourceCount > 0) {
                    append(" from $sourceCount source${if (sourceCount > 1) "s" else ""}")
                }
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
