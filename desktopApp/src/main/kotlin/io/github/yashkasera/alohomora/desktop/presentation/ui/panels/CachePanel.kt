package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.desktop.presentation.model.CacheRow
import io.github.yashkasera.alohomora.desktop.presentation.model.CacheUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.cacheSubtitle
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * One row per key, value inline.
 *
 * Replaces a "Keys" box of fixed height above a separate "Values" list. That split mirrored the wire
 * rather than the question being asked: keys arrive in the initial snapshot and each value arrives later
 * in its own frame, so the panel showed you a key in one place and made you find its value in another —
 * and the value list only ever held the keys you had already clicked, in click order. Values are now
 * fetched up front by `CacheViewModel`, which is what makes a single list possible.
 */
@Composable
fun CachePanel(cacheViewModel: CacheViewModel) {
    val uiState by cacheViewModel.uiState.collectAsState()
    val lazyListState = rememberLazyListState()
    // Ephemeral view state, so it stays local like ErrorsPanel's expandedId. One row at a time: these
    // are single values, not documents, and keeping several open turns the list back into a wall.
    var expandedKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Cache",
                subtitle = cacheSubtitle(uiState),
                showDivider = lazyListState.canScrollBackward,
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
                    query = uiState.query,
                    onQueryChange = cacheViewModel::onQueryChange,
                    // Says "value" explicitly, because matching inside values is the half a reader would
                    // not assume — and the subtitle's "loading" count is what admits the gap while
                    // values are still arriving.
                    placeholder = "Filter by key or value",
                    onClear = { cacheViewModel.onQueryChange("") },
                    modifier = Modifier.weight(1f),
                )
            }
            AlohomoraHorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.rows.isEmpty()) {
                    CacheEmptyState(uiState, onClearQuery = { cacheViewModel.onQueryChange("") })
                } else {
                    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
                        items(uiState.rows, key = { it.key }) { row ->
                            CacheEntryRow(
                                row = row,
                                expanded = row.key == expandedKey,
                                onToggle = {
                                    expandedKey = if (expandedKey == row.key) null else row.key
                                },
                            )
                            AlohomoraHorizontalDivider()
                        }
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
    }
}

@Composable
private fun CacheEntryRow(
    row: CacheRow,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (expanded) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            )
            .clickable(onClick = onToggle)
            .padding(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.md,
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (expanded) Icons.ChevronDown else Icons.ChevronRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.size(MaterialTheme.dimens.margin.sm))

            // Weighted rather than a fixed key column: the old 200.dp truncated long keys on a narrow
            // window and stranded whitespace on a wide one.
            Text(
                text = row.key,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(KEY_WEIGHT),
            )

            CacheValuePreview(row = row, modifier = Modifier.weight(VALUE_WEIGHT))
        }

        if (expanded) {
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // No per-key refresh. `RepositoryCacheInspector.getValue` answers from the cache primed
                // at connect — re-scanning per key would be quadratic in the number of preferences — so a
                // refresh button here could only ever return the value already on screen. Re-reading the
                // device means re-requesting initial state, which is a panel-level act, not a row-level
                // one.
                AlohomoraIconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(row.value.orEmpty())) },
                    enabled = row.value != null,
                ) {
                    Icon(
                        imageVector = Icons.Copy,
                        contentDescription = "Copy value",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                    )
                }
            }
            // jsonPrettify because a preference is a common place to stash a JSON blob, and a one-line
            // blob is unreadable. A non-JSON value passes through unchanged.
            //
            // NOT scrollable, and that is a correctness point rather than a preference: a LazyColumn
            // measures its items with an unbounded height, so a verticalScroll in here is measured with
            // an infinite max height and Compose throws. The row grows to fit and the list scrolls
            // instead — which is also the behaviour the reader wants, since expanding is a deliberate
            // request to see the whole value.
            AlohomoraCodeBlock(
                content = row.value ?: EMPTY_VALUE_LABEL,
                isScrollable = false,
                jsonPrettify = true,
            )
        }
    }
}

/**
 * The collapsed value, or why there is not one yet.
 *
 * Three outcomes, deliberately worded apart. The old panel printed the string "null" for the last two,
 * which reads as a value the app actually stored.
 */
@Composable
private fun CacheValuePreview(row: CacheRow, modifier: Modifier = Modifier) {
    val (text, muted) = when {
        row.isPending -> "loading…" to true
        row.isAbsent -> EMPTY_VALUE_LABEL to true
        row.value.isNullOrEmpty() -> "(empty)" to true
        else -> row.value to false
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            fontFamily = if (muted) FontFamily.Default else FontFamily.Monospace,
        ),
        color = if (muted) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun CacheEmptyState(state: CacheUiState, onClearQuery: () -> Unit) {
    if (state.totalCount == 0) {
        EmptyState(
            icon = Icons.Key,
            title = "No cache entries",
            subtitle = "Keys appear here once the connected app has written a preference.",
        )
    } else {
        EmptyState(
            icon = Icons.Search,
            title = "No entries match",
            // Names the pending count, because a value still in flight genuinely cannot be matched yet
            // and "no match" would otherwise look final.
            subtitle = buildString {
                append("${state.totalCount} keys captured.")
                if (state.pendingCount > 0) {
                    append(" ${state.pendingCount} still loading and cannot be searched yet.")
                }
            },
            action = {
                AlohomoraIconButton(onClick = onClearQuery) {
                    Icon(imageVector = Icons.Search, contentDescription = "Clear the filter")
                }
            },
        )
    }
}

/** Reads as a sentence rather than the literal "null", which looks like stored data. */
private const val EMPTY_VALUE_LABEL = "not set"

/** The key earns less width than its value: keys are short identifiers, values are the payload. */
private const val KEY_WEIGHT = 0.4f
private const val VALUE_WEIGHT = 0.6f
