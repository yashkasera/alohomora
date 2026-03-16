package io.github.yashkasera.alohomora.ui.components.jsonviewer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import kotlinx.coroutines.launch

@Composable
internal fun SearchToolbar(
    modifier: Modifier = Modifier,
    tree: JsonTree,
    visibleState: VisibleTreeState,
    listState: LazyListState
) {

    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Path>>(emptyList()) }
    var index by remember { mutableStateOf(0) }


    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val currentBorderColor = if (isFocused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline

    fun navigateTo(path: Path) {

        visibleState.expandParents(path)
        visibleState.expand(path)

        val row = visibleState.findRowIndex(path)

        if (row >= 0) {
            scope.launch {
                listState.animateScrollToItem(row)
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                border = BorderStroke(width = 1.dp, color = currentBorderColor),
                shape = MaterialTheme.shapes.extraSmall
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Icon(
            imageVector = Icons.Search,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = CanvasDarkGray.copy(alpha = 0.5f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = query,
            onValueChange = {

                query = it
                visibleState.searchQuery = it

                results = tree.searchIndex.search(it.lowercase())
                index = 0

                if (results.isNotEmpty()) {
                    navigateTo(results.first())
                }
            },
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CanvasDarkGray.copy(alpha = 0.5f),
                        )
                    }
                    innerTextField()
                }
            },
        )

        if (query.isNotEmpty()) {

            val count = results.size

            if (count > 0) {

                Text(
                    text = "${(index % count) + 1}/$count",
                    style = MaterialTheme.typography.bodySmall,
                    color = CanvasDarkGray.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                IconButton(
                    onClick = {

                        if (results.isEmpty()) return@IconButton

                        index = if (index <= 0) count - 1 else index - 1

                        val path = results[index % count]

                        navigateTo(path)
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CanvasDarkGray
                    )
                }

                IconButton(
                    onClick = {

                        if (results.isEmpty()) return@IconButton

                        index = (index + 1) % count

                        val path = results[index]

                        navigateTo(path)
                    },
                    modifier = Modifier.size(24.dp),
                ) {
                    Text(
                        text = "›",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CanvasDarkGray
                    )
                }

            } else {

                Text(
                    text = "No matches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            IconButton(
                onClick = {
                    query = ""
                    visibleState.searchQuery = ""
                    results = emptyList()
                    index = 0
                },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = Icons.X,
                    contentDescription = "Clear search",
                    modifier = Modifier.size(14.dp),
                    tint = CanvasDarkGray.copy(alpha = 0.5f),
                )
            }
        }
    }
}
