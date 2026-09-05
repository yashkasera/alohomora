package io.github.yashkasera.alohomora.common

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/**
 * Reads the property at [path] from an event, or `null` when it is absent.
 *
 * Goes through the same `takeUnless { it is JsonNull }` guard as [prettyProperties]: `recordEvent`
 * encodes a null property map as a non-null [JsonNull], so a no-property event must read as absent, not
 * as the literal element `null`. Journey selectors and assertions read properties **only** through this
 * — never by indexing a raw [JsonElement].
 *
 * [path] is a dotted path into nested objects (`user.id`); a segment that is not an object, or a key
 * that is missing, yields `null`. A resolved value that is itself [JsonNull] also reads as `null`.
 */
fun Event.propertyAt(path: String): JsonElement? {
    val root = properties?.takeUnless { it is JsonNull } ?: return null
    var current: JsonElement = root
    for (segment in path.split('.')) {
        val obj = current as? JsonObject ?: return null
        current = obj[segment]?.takeUnless { it is JsonNull } ?: return null
    }
    return current
}
