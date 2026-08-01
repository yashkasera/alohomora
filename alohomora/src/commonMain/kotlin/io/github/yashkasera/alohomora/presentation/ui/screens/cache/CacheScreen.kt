package io.github.yashkasera.alohomora.presentation.ui.screens.cache

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.domain.model.PreferenceEntry
import io.github.yashkasera.alohomora.domain.model.PreferenceType
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Database
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
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    /*AlohomoraIconButton(onClick = { *//* Clear Cache *//* }) {
                        Icon(Icons.Trash, contentDescription = "Clear Cache")
                    }*/
                }
            )
        },
        bottomBar = {
            CacheFooter(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                totalEntries = state.totalEntries,
                filteredCount = state.filteredCount,
                totalSize = totalSize,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = MaterialTheme.dimens.margin.xl)
        ) {

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

            // Search Bar
            SearchTextField(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            // Content
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
private fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    AlohomoraSearchTextField(
        query = query,
        onQueryChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = "Filter keys or values...",
    )
}

@Composable
private fun PreferencesList(
    entries: List<PreferenceEntry>,
) {
    val listState = rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            items(
                items = entries,
                key = { "${it.source.name}_${it.key}" },
            ) { entry ->
                PreferenceItem(entry = entry)
                AlohomoraHorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
        ScrollToTopButton(listState)
    }
}

@Composable
private fun PreferenceItem(
    entry: PreferenceEntry,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.margin.lg),
    ) {
        // Key row with type chip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.key,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))

            // Type chip
            // Was entry.type.name — a raw enum leaking into the UI.
            AlohomoraChip(label = entry.type.displayLabel())
        }

        Spacer(modifier = Modifier.height(6.dp)) // 6.dp intentional: tight preference value gap

        // Value display with type-specific styling
        val valueColor = when (entry.type) {
            PreferenceType.BOOLEAN -> MaterialTheme.colorScheme.tertiary
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

@Composable
private fun CacheFooter(
    modifier:Modifier = Modifier,
    totalEntries: Int,
    filteredCount: Int,
    totalSize: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                "$filteredCount of $totalEntries Keys Found ($totalSize)"
            } else {
                "$totalEntries Keys Found ($totalSize)"
            }
            Text(
                text = entriesText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

