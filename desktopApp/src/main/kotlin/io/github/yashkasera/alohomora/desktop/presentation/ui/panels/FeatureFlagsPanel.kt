package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.FeatureFlag
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.FeatureFlagUiState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.FeatureFlagViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.ToggleLeft
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun FeatureFlagsPanel(
    featureFlagsViewModel: FeatureFlagViewModel,
    searchFocusTrigger: Long = 0L,
) {
    val uiState by featureFlagsViewModel.uiState.collectAsState()
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) searchFocus.requestFocus()
    }
    val lazyListState = rememberLazyListState()
    var expandedKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Feature Flags",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = featureFlagsSubtitle(uiState),
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
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AlohomoraSearchTextField(
                    query = uiState.query,
                    onQueryChange = featureFlagsViewModel::onQueryChange,
                    placeholder = "Filter by key, value, source, or type",
                    onClear = { featureFlagsViewModel.onQueryChange("") },
                    modifier = Modifier.weight(1f).focusRequester(searchFocus),
                )
            }

            if (uiState.sources.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MaterialTheme.dimens.margin.xxl)
                        .padding(bottom = MaterialTheme.dimens.margin.sm)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    uiState.sources.forEach { source ->
                        AlohomoraFilterChip(
                            label = source,
                            selected = uiState.selectedSource == source,
                            onClick = { featureFlagsViewModel.onSourceSelected(source) },
                        )
                    }
                }
            }

            AlohomoraHorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                val filtered = uiState.filteredFlags
                if (filtered.isEmpty()) {
                    FeatureFlagsEmptyState(
                        uiState = uiState,
                        onClearQuery = { featureFlagsViewModel.onQueryChange("") },
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
                        items(filtered, key = { it.key }) { flag ->
                            FeatureFlagRow(
                                flag = flag,
                                expanded = flag.key == expandedKey,
                                onToggle = {
                                    expandedKey = if (expandedKey == flag.key) null else flag.key
                                },
                            )
                        }
                        fabClearanceItem()
                    }
                    ScrollToTopButton(lazyListState)
                }
            }
        }
    }
}

@Composable
private fun FeatureFlagRow(
    flag: FeatureFlag,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    AlohomoraCard(
        modifier = Modifier.fillMaxWidth(),
        colors = AlohomoraCardDefaults.colors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer,
        ),
        onClick = onToggle,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.md,
            ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(0.6f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.ChevronDown else Icons.ChevronRight,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Text(
                        text = flag.key,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false),
                        overflow = TextOverflow.Ellipsis,
                    )
                    flag.type?.let { type ->
                        AlohomoraChip(
                            label = type,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }

                    flag.source?.let { source ->
                        AlohomoraChip(
                            label = source,
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    }
                }

                FlagValuePreview(
                    flag = flag,
                    modifier = Modifier.weight(0.4f),
                )
            }

            AnimatedVisibility(expanded) {
                Column {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    Row(
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlohomoraCodeBlock(
                            modifier = Modifier.weight(1f),
                            content = flag.value,
                            isScrollable = false,
                            jsonPrettify = true,
                        )
                        AlohomoraIconButton(
                            onClick = {
                                clipboardManager.setText(
                                    /*AnnotatedString(
                                        flag.value + "\n" + flag.metadata,
                                    ),*/
                                    buildAnnotatedString {
                                        append("Key:")
                                        appendLine("`${flag.key}`")
                                        append("Value:")
                                        appendLine("`${flag.value}`")
                                        flag.metadata?.ifEmpty { null }?.let {
                                            appendLine("Metadata:")
                                            append("```${it}```")
                                        }
                                    },
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Copy,
                                contentDescription = "Copy value",
                                modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                            )
                        }
                    }

                    val meta = flag.metadata
                    if (!meta.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                        meta.forEach { (k, v) ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    text = "$k: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    text = v,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagValuePreview(flag: FeatureFlag, modifier: Modifier = Modifier) {
    val color = booleanColor(flag.value) ?: MaterialTheme.colorScheme.onSurface
    Text(
        text = flag.value,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        textAlign = TextAlign.End,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

@Composable
private fun booleanColor(value: String): Color? = when {
    value.equals("true", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
    value.equals("false", ignoreCase = true) -> MaterialTheme.colorScheme.error
    else -> null
}

@Composable
private fun FeatureFlagsEmptyState(uiState: FeatureFlagUiState, onClearQuery: () -> Unit) {
    if (uiState.totalCount == 0) {
        EmptyState(
            icon = Icons.ToggleLeft,
            title = "No feature flags",
            subtitle = "Flags appear here once the connected app calls Alohomora.recordFeatureFlag().",
            setup = "Alohomora.recordFeatureFlag(\n" +
                "    key = \"dark_mode\",\n" +
                "    value = \"true\",\n" +
                "    source = \"remote_config\",\n" +
                ")",
        )
    } else {
        EmptyState(
            icon = Icons.Search,
            title = "No flags match",
            subtitle = "${uiState.totalCount} flags captured.",
            action = {
                AlohomoraIconButton(onClick = onClearQuery) {
                    Icon(imageVector = Icons.Search, contentDescription = "Clear the filter")
                }
            },
        )
    }
}

private fun featureFlagsSubtitle(state: FeatureFlagUiState): String = buildString {
    val filtered = state.filteredFlags
    if (state.query.isBlank() && state.selectedSource == null) {
        append("${state.totalCount} flag${if (state.totalCount != 1) "s" else ""}")
    } else {
        append("${filtered.size} of ${state.totalCount}")
    }
    if (state.sources.isNotEmpty()) {
        append(" · ${state.sources.size} source${if (state.sources.size != 1) "s" else ""}")
    }
}
