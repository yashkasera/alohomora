package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.CacheState

/**
 * One cache entry as the panel renders it.
 *
 * The panel used to render `keys` and `values` as two separate lists because that is the shape the wire
 * delivers: `InitialStatePayload.cacheKeys` carries every key, and each value arrives later in its own
 * `CacheSnapshotMessage`. This type is what collapses the two back into one row, so the loading order of
 * the transport stops being the layout of the screen.
 */
data class CacheRow(
    val key: String,
    val value: String?,
    /**
     * Whether the device has answered for this key.
     *
     * Distinct from `value != null`, and the distinction is the whole reason this field exists:
     * `CacheState.values` maps a key to `String?`, so a key absent from the map ("not asked yet") and a
     * key mapped to null ("asked, and the device has nothing under it") are different facts that the old
     * panel rendered identically as the word "null".
     */
    val isLoaded: Boolean,
) {
    /** Requested, with the device's answer still in flight. */
    val isPending: Boolean get() = !isLoaded

    /** The device answered and holds no value under this key. */
    val isAbsent: Boolean get() = isLoaded && value == null
}

data class CacheUiState(
    val rows: List<CacheRow> = emptyList(),
    val query: String = "",
    /** Keys the device reported, before the query narrows them. */
    val totalCount: Int = 0,
    val loadedCount: Int = 0,
) {
    val shownCount: Int get() = rows.size

    val pendingCount: Int get() = totalCount - loadedCount
}

/**
 * Builds the rows for [query], newest state wins.
 *
 * Matches a key or a *loaded* value. Values that have not arrived cannot be searched — a limitation the
 * subtitle surfaces via the pending count rather than hiding, since a query that silently skips
 * unloaded entries would otherwise read as "no such value".
 */
fun CacheState.toCacheRows(query: String): List<CacheRow> {
    val needle = query.trim().lowercase()
    return keys.asSequence()
        .map { key ->
            CacheRow(
                key = key,
                value = values[key],
                // containsKey, not a null check: see CacheRow.isLoaded.
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

/** "24 keys · 3 shown · 2 loading". Pure so the string is testable. */
fun cacheSubtitle(state: CacheUiState): String = buildString {
    append("${state.totalCount} ${if (state.totalCount == 1) "key" else "keys"}")
    if (state.shownCount != state.totalCount) append(" · ${state.shownCount} shown")
    // Named because a pending value is also a value the query cannot reach yet.
    if (state.pendingCount > 0) append(" · ${state.pendingCount} loading")
}
