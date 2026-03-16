package io.github.yashkasera.alohomora.ui.components.jsonviewer

internal class SearchTrie {

    private val root = TrieNode()

    fun insert(token: String, path: Path) {

        var node = root

        token.forEach {

            node = node.children.getOrPut(it) {
                TrieNode()
            }
        }

        node.paths += path
    }

    fun search(prefix: String): List<Path> {

        var node = root

        prefix.forEach {

            node = node.children[it] ?: return emptyList()
        }

        return collect(node)
    }

    private fun collect(node: TrieNode): List<Path> {

        val result = mutableListOf<Path>()

        result += node.paths

        node.children.values.forEach {
            result += collect(it)
        }

        return result
    }
}

internal class TrieNode {

    val children = mutableMapOf<Char, TrieNode>()

    val paths = mutableListOf<Path>()
}
