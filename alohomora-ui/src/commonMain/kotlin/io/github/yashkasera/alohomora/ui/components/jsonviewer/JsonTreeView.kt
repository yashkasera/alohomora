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
import kotlinx.serialization.json.Json

@Composable
fun JsonTreeView(
    json: String,
    listState: LazyListState = rememberLazyListState(),
    parentContent: (LazyListScope.() -> Unit)? = null,
) {
    val element = remember { Json.parseToJsonElement(json) }
    val tree = remember { JsonTreeBuilder.build(element) }
    val visibleState = remember { VisibleTreeState(tree) }

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
            stickyHeader {
                SearchToolbar(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
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
        }
    }
}
