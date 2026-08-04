package io.github.yashkasera.alohomora.ui.components.jsonviewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
        ) {
            parentContent?.invoke(this)

            guardLet(element, tree, visibleState) { element, tree, visibleState ->
                stickyHeader {
                    SearchToolbar(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        tree = tree,
                        visibleState = visibleState,
                        listState = listState,
                    )
                }
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
}

fun <A, B, C> guardLet(a: A?, b: B?, c: C?, action: (A, B, C) -> Unit): Unit? {
    if (a != null && b != null && c != null) {
        action(a, b, c)
        return Unit
    }
    return null
}
