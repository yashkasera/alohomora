package io.github.yashkasera.alohomora.replay

/**
 * One editable header before the request goes back out. [key] is a stable identity so a row keeps its
 * text field state when siblings are added or removed; [included] `false` drops the header from the
 * replay entirely, letting the app's own client regenerate it (auth, signature, content-length).
 *
 * Shared by both consoles: the desktop replay sheet and the mobile replay screen edit headers as the
 * same structured rows, so the rows -> wire-map collapse must behave identically on both.
 */
data class ReplayHeaderRow(
    val key: Long,
    val name: String,
    val value: String,
    val included: Boolean,
)

/**
 * Collapses the editable rows back into the wire shape, preserving order and grouping repeated names
 * the same way [ReplayHeaderText.parse] does. Excluded rows and blank names are dropped rather than
 * sent as empty headers.
 */
fun buildReplayHeaderMap(rows: List<ReplayHeaderRow>): Map<String, List<String>> {
    val map = LinkedHashMap<String, MutableList<String>>()
    rows.forEach { row ->
        if (!row.included) return@forEach
        val name = row.name.trim()
        if (name.isEmpty()) return@forEach
        map.getOrPut(name) { mutableListOf() }.add(row.value.trim())
    }
    return map
}

/**
 * Seeds editable rows from a wire header map, one row per value so multi-value headers stay
 * individually editable. [nextKey] supplies the stable per-row identity.
 */
fun Map<String, List<String>>.toReplayHeaderRows(nextKey: () -> Long): List<ReplayHeaderRow> =
    entries.flatMap { (name, values) -> values.map { name to it } }
        .map { (name, value) -> ReplayHeaderRow(nextKey(), name, value, included = true) }
