package io.github.yashkasera.alohomora.common

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Longest attribute value kept, before truncation.
 *
 * Tracers cap attribute *count* (OpenTelemetry defaults to 128 per span) but none of them cap value
 * *length*, so an app that stuffs a response body into a span attribute would push a frame past
 * `DevToolsProtocol.MAX_PAYLOAD_BYTES` and kill the connection. The snapshot budget in
 * `DevToolsDefaults.SPAN_SNAPSHOT_LIMIT` is only arithmetically safe because of this cap.
 */
const val SPAN_ATTRIBUTE_VALUE_MAX_CHARS: Int = 4096

/** Suffix marking a value cut by [SPAN_ATTRIBUTE_VALUE_MAX_CHARS], matching the traffic bodies' convention. */
private const val TRUNCATION_SUFFIX = "…truncated"

/**
 * Normalises a raw span id to the form the rest of the code assumes, or null when there isn't one.
 *
 * Two jobs, both load-bearing:
 * - **Lowercase**, because grouping and parent lookup are string equality. A tracer that emits
 *   uppercase hex would put every span in its own trace and orphan every child.
 * - **All-zeros to null.** OpenTelemetry reports an absent parent as all-zero hex, not as a
 *   missing field. Treat that as a real id and no span is ever a root, so a whole trace renders as
 *   a flat list of orphans under a parent that does not exist.
 */
fun normalizeSpanId(raw: String?): String? {
    val trimmed = raw?.trim()?.lowercase()
    if (trimmed.isNullOrEmpty()) return null
    if (trimmed.all { it == '0' }) return null
    return trimmed
}

/**
 * Converts caller-supplied attributes to the [JsonElement] the entity stores, truncating any
 * oversized value.
 *
 * Returns null for null or empty input so an attribute-less span stores SQL NULL rather than `{}`,
 * matching how `Event.properties` behaves.
 */
fun spanAttributesToJson(attributes: Map<String, String>?): JsonElement? {
    if (attributes.isNullOrEmpty()) return null
    return JsonObject(attributes.mapValues { (_, value) -> JsonPrimitive(value.truncateAttribute()) })
}

private fun String.truncateAttribute(): String =
    if (length <= SPAN_ATTRIBUTE_VALUE_MAX_CHARS) {
        this
    } else {
        take(SPAN_ATTRIBUTE_VALUE_MAX_CHARS) + TRUNCATION_SUFFIX
    }
