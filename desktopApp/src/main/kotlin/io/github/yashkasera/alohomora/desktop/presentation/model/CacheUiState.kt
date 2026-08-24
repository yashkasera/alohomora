package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.CacheState

data class CacheRow(
    val key: String,
    val value: String?,
    val type: String?,
    val storeName: String?,
    val isEncrypted: Boolean,
    val isLoaded: Boolean,
) {
    val isPending: Boolean get() = !isLoaded
    val isAbsent: Boolean get() = isLoaded && value == null
    val isEditable: Boolean get() = type != null && type != "STRING_SET"
}

data class CacheUiState(
    val rows: List<CacheRow> = emptyList(),
    val query: String = "",
    val totalCount: Int = 0,
    val loadedCount: Int = 0,
    val hasStoreData: Boolean = false,
) {
    val shownCount: Int get() = rows.size
    val pendingCount: Int get() = totalCount - loadedCount
}

fun CacheState.toCacheRows(query: String): List<CacheRow> {
    val needle = query.trim().lowercase()
    if (stores.isNotEmpty()) {
        return stores.asSequence()
            .flatMap { store ->
                store.entries.asSequence().map { entry ->
                    CacheRow(
                        key = entry.key,
                        value = entry.value,
                        type = entry.type,
                        storeName = store.name,
                        isEncrypted = store.isEncrypted,
                        isLoaded = true,
                    )
                }
            }
            .filter { row ->
                needle.isEmpty() ||
                    row.key.lowercase().contains(needle) ||
                    row.value?.lowercase()?.contains(needle) == true
            }
            .toList()
    }
    return keys.asSequence()
        .map { key ->
            CacheRow(
                key = key,
                value = values[key],
                type = null,
                storeName = null,
                isEncrypted = false,
                isLoaded = values.containsKey(key),
            )
        }
        .filter { row ->
            needle.isEmpty() ||
                row.key.lowercase().contains(needle) ||
                row.value?.lowercase()?.contains(needle) == true
        }
        .toList()
}

fun cacheSubtitle(state: CacheUiState): String = buildString {
    append("${state.totalCount} ${if (state.totalCount == 1) "key" else "keys"}")
    if (state.shownCount != state.totalCount) append(" · ${state.shownCount} shown")
    if (state.pendingCount > 0) append(" · ${state.pendingCount} loading")
}
