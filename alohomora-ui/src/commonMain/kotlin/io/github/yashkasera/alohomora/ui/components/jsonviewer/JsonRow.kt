package io.github.yashkasera.alohomora.ui.components.jsonviewer

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

private val keyColor = Color(0xFF6366F1)      // indigo — matches brand primary
private val stringColor = Color(0xFFC2410C)   // burnt orange
private val numberColor = Color(0xFF059669)   // emerald — matches CanvasSuccessGreen
private val bracketColor = Color(0xFFA1A1AA)  // zinc-400

@Composable
internal fun LazyItemScope.JsonRow(
    node: JsonNode,
    row: Row,
    index: Int,
    visibleState: VisibleTreeState,
) {

    val comma = visibleState.hasSiblingAfter(index)
    val expanded = visibleState.isExpanded(node.path)
//    val indent  = (row.depth * 16).dp
    val indent by animateDpAsState((row.depth * 16).dp)
    val isContainer = node is JsonObjectNode || node is JsonArrayNode
    val isEmptyContainer = isContainer &&
        visibleState.tree.children[node.path]?.isEmpty() == true
    val isExpandable = isContainer && !isEmptyContainer

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded && isExpandable) 90f else 0f,
        label = "arrowRotation",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateItem()
            .padding(start = indent, top = 2.dp, bottom = 2.dp),
    ) {

        when (row.kind) {

            RowKind.OPEN -> {
                val previewText = remember(node.path) {
                    preview(node, visibleState.tree)
                }

                Text(
                    text = "▶",
                    modifier = Modifier
                        .rotate(arrowRotation)
                        .clickable(enabled = isExpandable) { visibleState.toggle(node.path) }
                        .padding(end = 6.dp),
                    color = if (isExpandable)
                        Color(0xFF52525B) else Color(0xFFD4D4D8),
                )

                node.key?.let { key ->

                    Text(
                        text = highlightText(
                            text = key,
                            query = visibleState.searchQuery,
                            normal = keyColor,
                        ),
                        color = keyColor,
                    )

                    Text(": ")
                }

                if (!expanded && previewText.isNotEmpty()) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp),
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = bracketColor)) {
                                append(if (node is JsonArrayNode) "[" else "{")
                            }
                            append(" ")
                            append(previewText)
                            append(" ")
                            withStyle(SpanStyle(color = bracketColor)) {
                                append(if (node is JsonArrayNode) "]" else "}")
                            }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                } else if (!expanded && isEmptyContainer) {
                    Text(
                        text = if (node is JsonArrayNode) "[ ]" else "{ }",
                        color = bracketColor,
                    )
                } else {
                    Text(
                        text = if (node is JsonArrayNode) "[" else "{",
                        color = bracketColor,
                    )
                }
            }

            RowKind.VALUE -> {
                Spacer(Modifier.width(16.dp))
                node.key?.let {
                    Text(
                        text = highlightText(
                            text = it,
                            query = visibleState.searchQuery,
                            normal = keyColor,
                        ),
                        color = keyColor,
                    )

                    Text(": ")
                }

                when (val value = (node as JsonValueNode).value) {
                    is Number -> {
                        Text(
                            text = highlightText(
                                text = value.toString(),
                                query = visibleState.searchQuery,
                                normal = numberColor,
                            ),
                            color = numberColor,
                        )
                    }

                    is String -> {
                        Text(
                            text = highlightText(
                                text = "\"$value\"",
                                query = visibleState.searchQuery,
                                normal = stringColor,
                            ),
                            color = stringColor,
                        )
                    }

                    else -> {
                        Text(
                            text = highlightText(
                                text = value.toString(),
                                query = visibleState.searchQuery,
                                normal = LocalContentColor.current,
                            ),
                        )
                    }
                }

                if (comma) {
                    Text(",", color = bracketColor)
                }
            }

            RowKind.CLOSE -> {

                Text(
                    text = if (node is JsonArrayNode) "]" else "}",
                    color = bracketColor,
                )
                if (comma) {
                    Text(",", color = bracketColor)
                }
            }
        }
    }
}
