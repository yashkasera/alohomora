package io.github.yashkasera.alohomora.presentation.ui.screens.featureflags

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import io.github.yashkasera.alohomora.ui.icons.SlidersHorizontal
import io.github.yashkasera.alohomora.ui.icons.ToggleLeft
import io.github.yashkasera.alohomora.ui.icons.ToggleRight
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FeatureFlagItem(
    flag: FeatureFlag,
    modifier: Modifier = Modifier,
) {
    val isTrue = flag.value.equals("true", ignoreCase = true)
    val isFalse = flag.value.equals("false", ignoreCase = true)
    val iconTint = when {
        isTrue -> MaterialTheme.alohomoraColors.success
        isFalse -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    val flagIcon = when {
        isTrue -> Icons.ToggleRight to MaterialShapes.Bun
        isFalse -> Icons.ToggleLeft to MaterialShapes.Pill
        else -> Icons.SlidersHorizontal to MaterialShapes.Cookie9Sided
    }

    AlohomoraCard(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(iconTint.copy(alpha = 0.12f), flagIcon.second.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = flagIcon.first,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = flag.key,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                Text(
                    modifier = Modifier.fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(MaterialTheme.dimens.margin.sm),
                    text = flag.value,
                    style = MaterialTheme.typography.labelMedium,
                    color = iconTint,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

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

                val hasChips = flag.type != null || flag.source != null
                if (hasChips) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    // FlowRow, not Row: a long source name ("Firebase Remote Config") must drop to
                    // its own line as an intact chip rather than squeeze and ellipsize beside the
                    // type chip.
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    ) {
                        flag.type?.let { type -> AlohomoraChip(label = type) }
                        flag.source?.let { source -> AlohomoraChip(label = source) }
                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun FeatureFlagItemPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                FeatureFlagItem(
                    flag = FeatureFlag(
                        key = "enable_new_checkout",
                        value = "true",
                        source = "Firebase",
                        type = "Boolean",
                    ),
                )
                FeatureFlagItem(
                    flag = FeatureFlag(
                        key = "dark_mode_v2",
                        value = "false",
                        source = "LaunchDarkly",
                        type = "Boolean",
                    ),
                )
                FeatureFlagItem(
                    flag = FeatureFlag(
                        key = "max_retry_count",
                        value = "5",
                        source = "Remote Config",
                        type = "Int",
                        metadata = mapOf("updated" to "2024-08-20", "env" to "prod"),
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun FeatureFlagItemLongValuePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            FeatureFlagItem(
                flag = FeatureFlag(
                    key = "experiment_config_json",
                    value = """{"variant": "B", "rollout": 0.25, "cohort": "new_users", "expires": "2025-01-01"}""",
                    source = "Remote Config",
                    type = "JSON",
                ),
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
            )
        }
    }
}

@Preview
@Composable
private fun FeatureFlagsFooterPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                FeatureFlagsFooter(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    totalCount = 42,
                    filteredCount = 42,
                    sourceCount = 3,
                )
                FeatureFlagsFooter(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    totalCount = 42,
                    filteredCount = 12,
                    sourceCount = 1,
                )
            }
        }
    }
}
