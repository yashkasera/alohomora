package io.github.yashkasera.alohomora.ui.components.jsonviewer

internal class SearchIndex {

    private val entries = mutableListOf<Entry>()

    fun insert(token: String, path: Path) {
        entries += Entry(token, path)
    }

    fun search(query: String): List<Path> {
        if (query.isBlank()) return emptyList()
        return entries
            .filter { it.token.contains(query) }
            .map { it.path }
    }

    private data class Entry(val token: String, val path: Path)
}
