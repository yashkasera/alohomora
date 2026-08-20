package io.github.yashkasera.alohomora.presentation.ui.screens.cache

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.domain.model.CacheEntry
import io.github.yashkasera.alohomora.domain.model.CacheSource
import io.github.yashkasera.alohomora.domain.model.CacheType
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CacheScreen(
    onBackClick: () -> Unit,
) {
    val viewModel: CacheViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val totalSize = remember(state.entries) { viewModel.getTotalSize() }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Cache",
                subtitle = "key-value store",
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
            CacheFooter(
                modifier = Modifier
                    .padding(MaterialTheme.dimens.margin.lg)
                    .testTag(AlohomoraTestTags.Cache.FOOTER),
                totalEntries = state.totalEntries,
                filteredCount = state.filteredCount,
                totalSize = totalSize,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AlohomoraSearchTextField(
                modifier = Modifier
                    .padding(horizontal = MaterialTheme.dimens.margin.md)
                    .testTag(AlohomoraTestTags.Chrome.SEARCH),
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            if (state.entries.isEmpty()) {
                EmptyState(
                    icon = Icons.Database,
                    title = "No Preferences Found",
                    subtitle = "Preference stores from your app will appear here",
                )
            } else if (state.filteredEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.Search,
                    title = "No Results",
                    subtitle = "Try adjusting your search query",
                )
            } else {
                PreferencesList(entries = state.filteredEntries)
            }
        }
    }
}

@Composable
private fun PreferencesList(
    entries: List<CacheEntry>,
) {
    val listState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AlohomoraTestTags.Cache.LIST),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            contentPadding = PaddingValues(MaterialTheme.dimens.margin.md),
        ) {
            items(
                items = entries,
                key = { "${it.source.name}_${it.key}" },
            ) { entry ->
                PreferenceItem(
                    entry = entry,
                    modifier = Modifier.testTag(AlohomoraTestTags.Cache.item(entry.key)),
                )
            }
            fabClearanceItem()
        }
        ScrollToTopButton(listState)
    }
}

@Composable
private fun PreferenceItem(
    entry: CacheEntry,
    modifier: Modifier = Modifier,
) {
    AlohomoraCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconTint = if (entry.isEncrypted) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Key,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = entry.key,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )

                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))

                    AlohomoraChip(label = entry.type.displayLabel())
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                val valueColor = when (entry.type) {
                    CacheType.BOOLEAN -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                val displayValue = if (entry.isEncrypted) {
                    "[encrypted]"
                } else {
                    entry.value
                }

                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodySmall,
                    color = valueColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CacheFooter(
    modifier: Modifier = Modifier,
    totalEntries: Int,
    filteredCount: Int,
    totalSize: String,
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
                text = "TOTAL ENTRIES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val entriesText = if (filteredCount != totalEntries) {
                "$filteredCount of $totalEntries keys ($totalSize)"
            } else {
                "$totalEntries keys ($totalSize)"
            }
            Text(
                text = entriesText,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun PreferenceItemPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                PreferenceItem(
                    entry = CacheEntry(
                        key = "user_auth_token",
                        value = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY...",
                        type = CacheType.STRING,
                        source = CacheSource.SHARED_PREFERENCES,
                    ),
                )
                PreferenceItem(
                    entry = CacheEntry(
                        key = "dark_mode_enabled",
                        value = "true",
                        type = CacheType.BOOLEAN,
                        source = CacheSource.SHARED_PREFERENCES,
                    ),
                )
                PreferenceItem(
                    entry = CacheEntry(
                        key = "encrypted_api_key",
                        value = "[encrypted]",
                        type = CacheType.STRING,
                        source = CacheSource.ENCRYPTED_SHARED_PREFERENCES,
                        isEncrypted = true,
                    ),
                )
                PreferenceItem(
                    entry = CacheEntry(
                        key = "launch_count",
                        value = "42",
                        type = CacheType.INT,
                        source = CacheSource.SHARED_PREFERENCES,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun CacheFooterPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                CacheFooter(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    totalEntries = 128,
                    filteredCount = 128,
                    totalSize = "4.2 KB",
                )
                CacheFooter(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    totalEntries = 128,
                    filteredCount = 12,
                    totalSize = "4.2 KB",
                )
            }
        }
    }
}

