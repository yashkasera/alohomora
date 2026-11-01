package io.github.yashkasera.alohomora.common

import kotlin.io.encoding.Base64

/**
 * Turns captured body bytes into the text a [TrafficEntry] stores, or decides they are not text.
 *
 * Shared by every capture path so they agree on what "binary" means. Two reasons it matters:
 * a binary body decoded leniently is corrupted (invalid sequences become U+FFFD), so replaying it
 * would send different bytes; and it is inflated, because JSON escapes each U+FFFD as three bytes
 * and each control byte as six. A 1 MB image grew past the 8 MiB frame cap that way, and the
 * desktop rejected the snapshot carrying it on every reconnect.
 */
object CapturedBody {

    /**
     * Decodes up to [maxBytes] of [bytes] as UTF-8, or returns null when they are not text.
     *
     * Strict: any invalid sequence means binary. A NUL byte does too, since text practically never
     * carries one and protobuf or image data that happens to be valid UTF-8 usually does. When the
     * body is longer than [maxBytes], the cut is moved back to a character boundary so a truncated
     * multi-byte character is not mistaken for corruption.
     */
    fun decodeText(bytes: ByteArray, maxBytes: Int = bytes.size): String? {
        val end = utf8Boundary(bytes, minOf(bytes.size, maxBytes))
        for (i in 0 until end) if (bytes[i] == 0.toByte()) return null
        return try {
            bytes.decodeToString(startIndex = 0, endIndex = end, throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            null
        }
    }

    /**
     * What a capture path stores for a request body: text when it is text, otherwise the bytes.
     *
     * [bytes] is the body, or at least its first `maxBytes + 1` bytes; [totalBytes] is its full
     * length, so a body over the cap is recognised without the caller materialising all of it.
     *
     * A body that is not text (strict decode fails, or [contentType] is known binary) is kept as
     * base64 when it fits under [maxBytes] in full, so replay can send the same bytes back. Over
     * the cap it gets the placeholder alone, as before: part of a binary body is no use to replay
     * and only inflates the frame. Text over the cap is cut on a character boundary and flagged.
     */
    fun requestBody(
        bytes: ByteArray,
        totalBytes: Long,
        maxBytes: Int,
        contentType: String?,
    ): CapturedRequestBody {
        val complete = totalBytes <= maxBytes && bytes.size.toLong() == totalBytes
        val text = if (isKnownBinary(contentType)) null else decodeText(bytes, maxBytes)
        return when {
            text == null && complete ->
                CapturedRequestBody(TrafficEntry.UNABLE_PARSE_MESSAGE, base64 = Base64.encode(bytes))

            text == null -> CapturedRequestBody(TrafficEntry.UNABLE_PARSE_MESSAGE)
            totalBytes > maxBytes ->
                CapturedRequestBody("$text\n…truncated ($totalBytes bytes total)", truncated = true)

            else -> CapturedRequestBody(text)
        }
    }

    /**
     * True for content types whose bodies are never text, so a capture path can skip reading them
     * at all. Absent or unknown types return false and are decided by [decodeText] instead: plenty
     * of APIs omit the header.
     */
    fun isKnownBinary(contentType: String?): Boolean {
        val type = contentType?.substringBefore(';')?.trim()?.lowercase() ?: return false
        return type.startsWith("image/") ||
            type.startsWith("audio/") ||
            type.startsWith("video/") ||
            type.startsWith("font/") ||
            type in BINARY_TYPES ||
            type.endsWith("+protobuf") ||
            type.startsWith("application/grpc")
    }

    /**
     * Stored in place of a response body that a capture path must not read: one that never ends
     * (server-sent events) or that only the app may consume (a Ktor streaming call). Distinct from
     * [TrafficEntry.UNABLE_PARSE_MESSAGE], because nothing was read, let alone failed to parse.
     */
    const val STREAMED_PLACEHOLDER = "Streamed body, not captured."

    /**
     * True for `text/event-stream`. Such a response stays open for as long as the server has
     * events, so reading its body for capture blocks until the stream ends, which is never for a
     * live feed. OkHttp's HttpLoggingInterceptor special-cases the same type for the same reason.
     */
    fun isEventStream(contentType: String?): Boolean =
        contentType?.substringBefore(';')?.trim()?.equals("text/event-stream", ignoreCase = true) == true

    private val BINARY_TYPES = setOf(
        "application/octet-stream",
        "application/protobuf",
        "application/x-protobuf",
        "application/vnd.google.protobuf",
        "application/pdf",
        "application/zip",
        "application/gzip",
        "application/x-gzip",
        "application/wasm",
    )

    /** Largest index <= [end] that does not split a UTF-8 sequence. */
    private fun utf8Boundary(bytes: ByteArray, end: Int): Int {
        if (end >= bytes.size) return end
        var i = end
        // Step back over continuation bytes (10xxxxxx) to the start of the cut character, which
        // is then excluded. At most three steps for valid UTF-8; the bound keeps garbage cheap.
        while (i > 0 && end - i < 4 && (bytes[i].toInt() and 0xC0) == 0x80) i--
        return i
    }
}

/**
 * A request body as [CapturedBody.requestBody] decided to store it.
 *
 * @property text what [TrafficEntry.requestBody] stores: the text, or the placeholder for a body
 *   that is not text
 * @property base64 the complete body for [TrafficEntry.requestBodyBase64], or null
 * @property truncated true when [text] is only the leading part of the body
 */
data class CapturedRequestBody(
    val text: String,
    val base64: String? = null,
    val truncated: Boolean = false,
)
