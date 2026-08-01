package io.github.yashkasera.alohomora.replay

import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TraceEntry

/**
 * Why a captured trace cannot be replayed.
 *
 * Surfaced instead of a disabled button with no explanation: every one of these has the same
 * symptom — the replay action is unavailable — and very different causes, so the console has to
 * be able to say which one applies.
 */
enum class ReplayBlockedReason {

    /** No URL or no method, so there is nothing to send. */
    INCOMPLETE_TRACE,

    /**
     * The body was never captured as text — a multipart upload, a streaming body, or a one-shot
     * body the interceptor deliberately refused to consume. What is stored is a placeholder, and
     * replaying it would send the literal string `Cannot parse body` to the server.
     */
    UNPARSEABLE_BODY,

    /**
     * The body exceeded the capture cap and was stored truncated.
     *
     * The most dangerous case to allow: a truncated JSON body still looks like a body, so
     * replaying it would send silently corrupted data and blame the server for rejecting it.
     */
    TRUNCATED_BODY,
    ;

    val message: String
        get() = when (this) {
            INCOMPLETE_TRACE -> "This trace has no URL or method to replay."
            UNPARSEABLE_BODY ->
                "The request body was not captured as text (multipart, streaming or one-shot), " +
                    "so it cannot be reproduced."
            TRUNCATED_BODY ->
                "The request body was too large to capture in full. Replaying the truncated copy " +
                    "would send incomplete data."
        }
}

/** Header handling for replayed requests. */
object ReplayHeaders {

    /**
     * Headers dropped before a replay goes out, because the client derives them from the request
     * it is actually sending.
     *
     * Forwarding the captured values instead is worse than useless: `Content-Length` from the
     * original body contradicts an edited one, and `Host` from the original URL contradicts an
     * edited URL. Both produce protocol-level errors that look nothing like their cause.
     */
    val STRIPPED: Set<String> = setOf(
        "content-length",
        "host",
        "connection",
        "transfer-encoding",
        "expect",
        "upgrade",
        "keep-alive",
        "proxy-connection",
    )

    /**
     * Extra header names the host app wants dropped on replay, so its own interceptors put them
     * back.
     *
     * Assign at startup. A signature or nonce header that is not *secret* is not redacted at
     * capture time, so it survives into the trace and would be forwarded verbatim — leaving an
     * interceptor that uses `addHeader` to append a second, correct value beside the stale one.
     * Naming it here removes that ambiguity.
     */
    var additionalStripList: Set<String> = emptySet()

    private fun isStripped(name: String): Boolean {
        val lower = name.lowercase()
        return lower in STRIPPED || additionalStripList.any { it.equals(name, ignoreCase = true) }
    }

    /**
     * Returns [headers] with hop-by-hop and recomputed names removed, and with every redacted
     * value dropped.
     *
     * Dropping redacted values matters more than it looks: sending the literal `[REDACTED]` as an
     * `Authorization` value produces a 401 that reads like a genuine auth failure. Removing the
     * header entirely lets the app's auth interceptor add the real one.
     */
    fun sanitize(headers: Map<String, List<String>>?): Map<String, List<String>> {
        if (headers.isNullOrEmpty()) return emptyMap()
        return headers.asSequence()
            .filterNot { isStripped(it.key) }
            .map { (name, values) -> name to values.filterNot { it == HeaderRedaction.REDACTED } }
            .filter { (_, values) -> values.isNotEmpty() }
            .toMap()
    }
}

/**
 * Converts between the header map on the wire and the `Name: value` text both consoles edit.
 *
 * Shared rather than written twice because the two directions have to agree exactly: a round trip
 * through the editor that silently dropped or reordered a header would change the request the user
 * believes they are replaying.
 */
object ReplayHeaderText {

    /** Renders [headers] one `Name: value` pair per line, repeating the name for multi-value headers. */
    fun render(headers: Map<String, List<String>>): String =
        headers.entries
            .flatMap { (name, values) -> values.map { "$name: $it" } }
            .joinToString("\n")

    /**
     * Parses edited header text back into a map, preserving order and grouping repeated names.
     *
     * Lines without a colon are dropped rather than guessed at, and blank lines are ignored, so a
     * half-typed line cannot turn into a header with an empty name.
     */
    fun parse(text: String): Map<String, List<String>> {
        val parsed = LinkedHashMap<String, MutableList<String>>()
        text.lineSequence().forEach { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@forEach
            val name = line.substring(0, separator).trim()
            val value = line.substring(separator + 1).trim()
            if (name.isEmpty()) return@forEach
            parsed.getOrPut(name) { mutableListOf() }.add(value)
        }
        return parsed
    }
}

/**
 * Returns why this trace cannot be replayed, or null when it can be.
 */
fun TraceEntry.replayBlockedReason(): ReplayBlockedReason? = when {
    url.isNullOrBlank() || method.isNullOrBlank() -> ReplayBlockedReason.INCOMPLETE_TRACE
    requestBody == TraceEntry.UNABLE_PARSE_MESSAGE -> ReplayBlockedReason.UNPARSEABLE_BODY
    requestBodyTruncated -> ReplayBlockedReason.TRUNCATED_BODY
    else -> null
}

/**
 * Builds a replayable request from this trace, or returns null when [replayBlockedReason] says it
 * cannot be replayed.
 */
fun TraceEntry.toReplayRequest(): ReplayRequest? {
    if (replayBlockedReason() != null) return null
    return ReplayRequest(
        sourceTraceId = id,
        method = method!!.uppercase(),
        url = url!!,
        headers = ReplayHeaders.sanitize(requestHeaders),
        body = requestBody,
        contentType = requestContentType,
    )
}
