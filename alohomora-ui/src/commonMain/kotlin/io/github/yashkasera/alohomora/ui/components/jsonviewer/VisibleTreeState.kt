package io.github.yashkasera.alohomora.ui.components.jsonviewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue

internal class VisibleTreeState(
    internal val tree: JsonTree,
) {

    internal val rows = mutableStateListOf<Row>()

    private val expanded = mutableStateSetOf<Path>()

    internal var searchQuery by mutableStateOf("")

    init {

        rows += Row(tree.root, 0, RowKind.OPEN)

        expand(tree.root)
    }

    internal fun toggle(path: Path) {

        if (expanded.contains(path)) {
            collapse(path)
        } else {
            expand(path)
        }
    }

    internal fun expand(path: Path) {

        val index = rows.indexOfFirst { it.path == path && it.kind == RowKind.OPEN }
        if (index == -1) return

        val depth = rows[index].depth
        val children = tree.children[path] ?: return

        expanded += path

        var insert = index + 1

        children.forEach {

            val node = tree.nodes[it]!!

            val kind =
                if (node is JsonValueNode)
                    RowKind.VALUE
                else
                    RowKind.OPEN

            rows.add(insert, Row(it, depth + 1, kind))

            insert++
        }

        rows.add(insert, Row(path, depth, RowKind.CLOSE))
    }

    private fun collapse(path: Path) {

        val index = rows.indexOfFirst { it.path == path && it.kind == RowKind.OPEN }
        if (index == -1) return

        val depth = rows[index].depth

        expanded -= path

        var i = index + 1

        while (i < rows.size && rows[i].depth > depth) {
            expanded -= rows[i].path
            rows.removeAt(i)
        }

        if (i < rows.size && rows[i].kind == RowKind.CLOSE) {
            rows.removeAt(i)
        }
    }

    internal fun findRowIndex(path: Path): Int {

        return rows.indexOfFirst { it.path == path }
    }

    internal fun isExpanded(path: Path): Boolean {
        return expanded.contains(path)
    }

    internal fun expandParents(path: Path) {

        var current = path

        while (true) {

            val parent = parentOf(current) ?: break

            if (!expanded.contains(parent)) {
                expand(parent)
            }

            current = parent
        }
    }

    private fun parentOf(path: Path): Path? {

        val dot = path.lastIndexOf('.')
        val bracket = path.lastIndexOf('[')

        val idx = maxOf(dot, bracket)

        if (idx <= 0) return null

        return path.substring(0, idx)
    }

    internal fun hasSiblingAfter(index: Int): Boolean {

        if (index >= rows.lastIndex) return false

        val current = rows[index]
        val next = rows[index + 1]

        // same level siblings
        if (next.depth == current.depth) return true

        // closing brace followed by sibling
        if (current.kind == RowKind.CLOSE && next.depth == current.depth) {
            return true
        }

        return false
    }
}

internal data class Row(
    val path: Path,
    val depth: Int,
    val kind: RowKind,
)

internal enum class RowKind {
    OPEN,
    VALUE,
    CLOSE
}
