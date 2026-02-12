package io.github.yashkasera.alohomora.devtools

internal class DevToolsStreamAdapter<T>(
    private val keySelector: (T) -> Long?,
) {
    private var lastKey: Long = Long.MIN_VALUE

    fun filterNew(items: List<T>): List<T> {
        if (items.isEmpty()) return emptyList()
        val newItems = items
            .mapNotNull { item ->
                val key = keySelector(item) ?: return@mapNotNull null
                if (key > lastKey) item to key else null
            }
            .sortedBy { it.second }
        if (newItems.isEmpty()) return emptyList()
        lastKey = newItems.last().second
        return newItems.map { it.first }
    }

    fun seed(items: List<T>) {
        val maxKey = items.mapNotNull { keySelector(it) }.maxOrNull() ?: return
        if (maxKey > lastKey) {
            lastKey = maxKey
        }
    }
}
