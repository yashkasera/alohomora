package io.github.yashkasera.alohomora.ui.components.jsonviewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButtonStyle
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Composable
fun JsonTreeView(
    json: String,
    listState: LazyListState = rememberLazyListState(),
    parentContent: (LazyListScope.() -> Unit)? = null,
) {
    // Keyed on `json`, and that key is load-bearing. Bare `remember` survives a change of argument in
    // the same composition slot, so reusing this viewer for a second payload — switching traffic
    // entries inside an open detail sheet, or selecting another span's attributes — kept showing the
    // *previous* JSON. Silent, and indistinguishable from the device having sent the wrong body.
    // The expansion state is keyed too: it indexes into `tree`, so keeping it across a new document
    // would expand unrelated nodes.
    val element: JsonElement? = remember(json) {
        try {
            Json.parseToJsonElement(json)
        } catch (e: Exception) {
            null
        }
    }
    val tree = remember(element) { element?.let(JsonTreeBuilder::build) }
    val visibleState = remember(tree) { tree?.let(::VisibleTreeState) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Path>>(emptyList()) }

    val scope = rememberCoroutineScope()
    var index by remember { mutableStateOf(0) }

    fun navigateTo(path: Path) {
        visibleState?.expandParents(path) ?: return
        visibleState.expand(path)
        val row = visibleState.findRowIndex(path)
        if (row >= 0) {
            scope.launch { listState.animateScrollToItem(row) }
        }
    }

    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
        ),
        LocalContentColor provides MaterialTheme.colorScheme.onBackground,
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = MaterialTheme.colorScheme.primary,
            backgroundColor = MaterialTheme.colorScheme.secondary,
        ),
    ) {
        Box() {
            Column(
                modifier = Modifier.fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        MaterialTheme.shapes.small,
                    ),
            ) {
                guardLetCompose(element, tree, visibleState) { _, tree, visibleState ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AlohomoraSearchTextField(
                            modifier = Modifier.weight(1f)
                                .padding(MaterialTheme.dimens.margin.sm),
                            query = query,
                            onQueryChange = {
                                query = it
                                visibleState.searchQuery = it
                                results = tree.searchIndex.search(it.lowercase())
                                index = 0
                                if (results.isNotEmpty()) navigateTo(results.first())
                            },
                        )
                        AnimatedVisibility(query.isNotEmpty()) {
                            Row(
                                modifier = Modifier.padding(end = MaterialTheme.dimens.margin.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                val count = results.size
                                if (count > 0) {
                                    Text(
                                        text = "${(index % count) + 1}/$count",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.sm),
                                    )
                                } else {
                                    Text(
                                        text = "No matches",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.sm),
                                    )
                                }
                            }
                        }
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(vertical = MaterialTheme.dimens.margin.md),
                ) {
                    parentContent?.invoke(this)
                    guardLet(element, tree, visibleState) { _, tree, visibleState ->
                        itemsIndexed(
                            visibleState.rows,
                            key = { _, row -> row.path + row.kind },
                        ) { index, row ->

                            val node = tree.nodes[row.path]!!
                            JsonRow(
                                node = node,
                                row = row,
                                index = index,
                                visibleState = visibleState,
                            )
                        }
                    } ?: run {
                        item {
                            AlohomoraCodeBlock(
                                content = json,
                                isScrollable = false,
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = query.isNotBlank(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(MaterialTheme.dimens.margin.xl),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AlohomoraIconButton(
                        style = AlohomoraIconButtonStyle.FILLED,
                        onClick = {
                            if (results.isEmpty()) return@AlohomoraIconButton
                            index = if (index <= 0) results.size - 1 else index - 1
                            navigateTo(results[index % results.size])
                        },
                        modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                    ) {
                        Icon(
                            imageVector = Icons.ChevronDown,
                            contentDescription = null,
                            modifier = Modifier
                                .rotate(180f)
                                .size(MaterialTheme.dimens.icon.md),
                        )
                    }

                    AlohomoraIconButton(
                        style = AlohomoraIconButtonStyle.FILLED,
                        onClick = {
                            if (results.isEmpty()) return@AlohomoraIconButton
                            index = (index + 1) % results.size
                            navigateTo(results[index])
                        },
                        modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                    ) {
                        Icon(
                            imageVector = Icons.ChevronDown,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                    }
                }
            }
        }
    }
}

fun <A, B, C> guardLet(a: A?, b: B?, c: C?, action: (A, B, C) -> Unit): Unit? {
    if (a != null && b != null && c != null) {
        action(a, b, c)
        return Unit
    }
    return null
}

@Composable
fun <A, B, C, T> T.guardLetCompose(a: A?, b: B?, c: C?, action: @Composable T.(A, B, C) -> Unit) {
    if (a != null && b != null && c != null) {
        action(a, b, c)
    }
    return
}
