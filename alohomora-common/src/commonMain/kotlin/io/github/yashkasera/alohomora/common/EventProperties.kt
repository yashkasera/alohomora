package io.github.yashkasera.alohomora.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

private val prettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

/**
 * [Event.properties] as indented JSON, or `"{}"` when the event carried none.
 *
 * Shared rather than duplicated because the `takeUnless { it is JsonNull }` guard is load-bearing and
 * was previously present on one side only. `Alohomora.recordEvent` encodes a null property map through
 * `encodeToJsonElement`, which produces a non-null [JsonNull], so `properties?.toString() ?: "{}"`
 * renders the literal word "null" — the mobile row documents that trap while the mobile detail sheet
 * fell into it.
 *
 * Falls back to `toString()` rather than throwing. The desktop renders whatever the device sent, and a
 * formatter is not the place to fail a whole row over an element it cannot re-encode.
 */
fun Event.prettyProperties(): String =
    properties
        ?.takeUnless { it is JsonNull }
        ?.let { element ->
            runCatching { prettyJson.encodeToString(JsonElement.serializer(), element) }
                .getOrElse { element.toString() }
        }
        ?: "{}"

/**
 * Clamps a payload to [max] lines, reporting what it hid.
 *
 * Truncation has to announce itself: a silently cut payload reads as an event that genuinely carried
 * only six keys. See `EventItem` for why the row clamps rather than scrolls.
 */
fun String.clampLines(max: Int): String {
    val lines = lines()
    if (lines.size <= max) return this
    return lines.take(max).joinToString("\n") + "\n… ${lines.size - max} more lines"
}
