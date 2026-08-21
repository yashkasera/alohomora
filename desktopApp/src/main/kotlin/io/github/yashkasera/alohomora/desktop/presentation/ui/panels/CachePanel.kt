package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.model.CacheRow
import io.github.yashkasera.alohomora.desktop.presentation.model.CacheUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.cacheSubtitle
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.LocalCopyFeedback
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.CacheViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun CachePanel(
    cacheViewModel: CacheViewModel,
    searchFocusTrigger: Long = 0L,
) {
    val uiState by cacheViewModel.uiState.collectAsState()
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) searchFocus.requestFocus()
    }
    val lazyListState = rememberLazyListState()
    var expandedKey by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Cache",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = cacheSubtitle(uiState),
                actions = {
                    if (uiState.hasStoreData) {
                        AlohomoraIconButton(onClick = { cacheViewModel.refresh() }) {
                            Icon(
                                imageVector = Icons.RefreshCw,
                                contentDescription = "Refresh cache",
                                modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                            )
                        }
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
                    query = uiState.query,
                    onQueryChange = cacheViewModel::onQueryChange,
                    onClear = { cacheViewModel.onQueryChange("") },
                    modifier = Modifier.weight(1f).focusRequester(searchFocus),
                )
            }

            AlohomoraHorizontalDivider()

            Box(modifier = Modifier.fillMaxSize()) {
                if (uiState.rows.isEmpty()) {
                    CacheEmptyState(uiState, onClearQuery = { cacheViewModel.onQueryChange("") })
                } else {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                        contentPadding = PaddingValues(
                            MaterialTheme.dimens.margin.md,
                        ),
                    ) {
                        items(uiState.rows, key = { it.key }) { row ->
                            CacheEntryRow(
                                row = row,
                                expanded = row.key == expandedKey,
                                onToggle = {
                                    expandedKey = if (expandedKey == row.key) null else row.key
                                },
                                onUpdate = { newValue ->
                                    val store = row.storeName ?: return@CacheEntryRow
                                    val type = row.type ?: return@CacheEntryRow
                                    cacheViewModel.updateValue(store, row.key, newValue, type)
                                },
                                onDelete = {
                                    val store = row.storeName ?: return@CacheEntryRow
                                    cacheViewModel.deleteValue(store, row.key)
                                    expandedKey = null
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
private fun CacheEntryRow(
    row: CacheRow,
    expanded: Boolean,
    onToggle: () -> Unit,
    onUpdate: (String?) -> Unit,
    onDelete: () -> Unit,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val copyFeedback = LocalCopyFeedback.current
    AlohomoraCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = if (expanded) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        ),
        onClick = onToggle,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.md,
            ),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimens.icon.xl)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.ChevronDown else Icons.ChevronRight,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text(
                    text = row.key,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(KEY_WEIGHT),
                )

                row.type?.let { type ->
                    TypeBadge(type)
                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                }

                CacheValuePreview(row = row, modifier = Modifier.weight(VALUE_WEIGHT))
            }

            AnimatedVisibility(expanded) {
                Column {
                    Row(
                        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlohomoraCodeBlock(
                            modifier = Modifier.weight(1f),
                            content = row.value ?: EMPTY_VALUE_LABEL,
                            isScrollable = false,
                            jsonPrettify = true,
                        )
                        AlohomoraIconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(row.value.orEmpty()))
                                copyFeedback("Copied to clipboard")
                            },
                            enabled = row.value != null,
                        ) {
                            Icon(
                                imageVector = Icons.Copy,
                                contentDescription = "Copy value",
                                modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                            )
                        }
                        if (row.isEditable) {
                            AlohomoraIconButton(onClick = onDelete) {
                                Icon(
                                    imageVector = Icons.Trash,
                                    contentDescription = "Delete entry",
                                    modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    if (row.isEditable) {
                        CacheValueEditor(
                            currentValue = row.value,
                            type = row.type!!,
                            onSubmit = onUpdate,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CacheValueEditor(
    currentValue: String?,
    type: String,
    onSubmit: (String?) -> Unit,
) {
    when (type) {
        "BOOLEAN" -> {
            val checked = currentValue.equals("true", ignoreCase = true)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (checked) "true" else "false",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                AlohomoraSwitch(
                    checked = checked,
                    onCheckedChange = { onSubmit(it.toString()) },
                )
            }
        }

        "INT", "LONG", "FLOAT" -> {
            var draft by remember(currentValue) { mutableStateOf(currentValue.orEmpty()) }
            var isError by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraTextField(
                    value = draft,
                    onValueChange = { newVal ->
                        draft = newVal
                        isError = !isValidNumeric(newVal, type)
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = "Enter $type value",
                    isError = isError,
                    supportingText = if (isError) "Invalid $type" else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isError && draft != currentValue) onSubmit(draft)
                        },
                    ),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                AlohomoraIconButton(
                    onClick = { if (!isError && draft != currentValue) onSubmit(draft) },
                    enabled = !isError && draft != currentValue,
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                    )
                }
            }
        }

        "STRING" -> {
            var draft by remember(currentValue) { mutableStateOf(currentValue.orEmpty()) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = "Enter value",
                    singleLine = true,
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (draft != currentValue) onSubmit(draft)
                        },
                    ),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                AlohomoraIconButton(
                    onClick = { if (draft != currentValue) onSubmit(draft) },
                    enabled = draft != currentValue,
                ) {
                    Icon(
                        imageVector = Icons.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                    )
                }
            }
        }
    }
}

private fun isValidNumeric(value: String, type: String): Boolean {
    if (value.isEmpty()) return false
    return try {
        when (type) {
            "INT" -> {
                value.toInt(); true
            }

            "LONG" -> {
                value.toLong(); true
            }

            "FLOAT" -> {
                value.toFloat(); true
            }

            else -> false
        }
    } catch (_: NumberFormatException) {
        false
    }
}

@Composable
private fun TypeBadge(type: String) {
    val label = when (type) {
        "STRING" -> "TEXT"
        "BOOLEAN" -> "BOOL"
        "INT" -> "INT"
        "LONG" -> "LONG"
        "FLOAT" -> "FLOAT"
        "STRING_SET" -> "SET"
        else -> type
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.secondaryContainer,
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

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
        style = MaterialTheme.typography.labelSmall,
        textAlign = TextAlign.End,
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

private const val EMPTY_VALUE_LABEL = "not set"
private const val KEY_WEIGHT = 0.4f
private const val VALUE_WEIGHT = 0.6f
