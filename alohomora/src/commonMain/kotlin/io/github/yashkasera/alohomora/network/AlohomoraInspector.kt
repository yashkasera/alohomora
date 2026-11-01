package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.CapturedBody
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.devtools.MOCK_ID_HEADER
import io.github.yashkasera.alohomora.devtools.NetworkRuleEngine
import io.github.yashkasera.alohomora.replay.ReplayMarker
import io.ktor.client.plugins.api.SendingRequest
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay

internal val AlohomoraRequestKey = AttributeKey<TrafficEntry>("AlohomoraRequest")

/**
 * Marks a Ktor call as the replay of the trace whose id this holds.
 *
 * An attribute rather than a header because an attribute stays client-side. A header would have to be
 * stripped on the way out, and that is only safe when [AlohomoraInspector] runs before whatever signs
 * the request — otherwise the signature covers a header that is then removed and the replay fails auth
 * for a reason nothing on screen explains. An attribute has no ordering hazard at all.
 *
 * Set it yourself only if you write a custom Ktor handler instead of using `ktorReplayHandler`.
 */
val AlohomoraReplayOfKey: AttributeKey<String> = AttributeKey("AlohomoraReplayOf")

/** Upper bound on a captured body, matching the OkHttp interceptor. */
private const val MAX_BODY_CHARS = 1_000_000

/** Set once the request body has been recorded, so a redirect or retry does not overwrite it. */
private val AlohomoraBodyCapturedKey = AttributeKey<Unit>("AlohomoraBodyCaptured")

private fun String.truncateForCapture(): String =
    if (length <= MAX_BODY_CHARS) this
    else take(MAX_BODY_CHARS) + "\n…truncated ($length chars total)"

private fun String.wasTruncatedForCapture(): Boolean = length > MAX_BODY_CHARS

/**
 * Records the request body from the serialized [content], plus the content type and length when
 * onRequest's headers did not carry them: before serialization there is no Content-Length at all,
 * and a type Ktor picks itself (a String body's text/plain, ContentNegotiation's default) exists
 * only on the OutgoingContent.
 */
private fun TrafficEntry.captureRequestBody(content: OutgoingContent) {
    if (requestContentType == null) requestContentType = content.contentType?.toString()
    if (requestSize == null) requestSize = content.contentLength

    when (content) {
        is TextContent -> {
            requestBodyTruncated = content.text.wasTruncatedForCapture()
            requestBody = content.text.truncateForCapture()
        }

        is OutgoingContent.ByteArrayContent -> {
            val bytes = content.bytes()
            // Strict, like the other capture paths: a lenient decode corrupts a binary body, so its
            // replay would send different bytes. Unparseable is refused by replay. Bounded, so only
            // the stored prefix of a large upload is ever materialised as a String.
            val text = if (CapturedBody.isKnownBinary(content.contentType?.toString())) {
                null
            } else {
                CapturedBody.decodeText(bytes, MAX_BODY_CHARS)
            }
            requestBodyTruncated = text != null && bytes.size > MAX_BODY_CHARS
            requestBody = when {
                text == null -> TrafficEntry.UNABLE_PARSE_MESSAGE
                requestBodyTruncated -> "$text\n…truncated (${bytes.size} bytes total)"
                else -> text
            }
        }

        // Null, not UNABLE_PARSE_MESSAGE: a GET has no body, and calling absence a parse failure
        // made every bodyless request render "Cannot parse body". Ktor's EmptyContent is a NoContent.
        is OutgoingContent.NoContent -> requestBody = null

        // Streaming, channel and multipart bodies can only be read by consuming them, which would
        // send the real request empty. Deliberately not content.toString(), which yields a bare
        // class name that reads like a captured body but is not one.
        else -> requestBody = if (content.contentLength == 0L) null else TrafficEntry.UNABLE_PARSE_MESSAGE
    }
}

/**
 * True when lenient decoding has already mangled bytes that were not text. `bodyAsText` swaps
 * invalid sequences for U+FFFD rather than failing, so this is the only trace left of them.
 */
private fun String.looksDecodedFromBinary(): Boolean = indexOf('�') >= 0 || indexOf('\u0000') >= 0

@OptIn(ExperimentalUuidApi::class)
val AlohomoraInspector = createClientPlugin("AlohomoraInspector") {
    // The body is not read here. onRequest runs before ContentNegotiation serializes, so for
    // `setBody(dto)` the content is still the DTO object; reading it as "no body" let replay send
    // an empty POST. The body is captured at SendingRequest below instead.
    onRequest { request, _ ->
        // Wrapped: a debug tool must not be able to fail the host app's request.
        runCatching {
            // The attribute is the supported channel and costs nothing. The header is honoured for
            // custom handlers that have no attribute to set, and is removed before the headers are
            // captured so it neither reaches the server nor appears as a header the app did not set.
            val headerReplayOf = request.headers[ReplayMarker.HEADER]
                ?.also { request.headers.remove(ReplayMarker.HEADER) }
            val replayOf = request.attributes.getOrNull(AlohomoraReplayOfKey) ?: headerReplayOf

            val query = request.url.parameters.entries()
                .joinToString("&") { "${it.key}=${it.value.joinToString(",")}" }
                .takeIf { it.isNotEmpty() }

            val entity = TrafficEntry(
                id = Uuid.random().toString(),
                url = request.url.buildString(),
                method = request.method.value,
                scheme = request.url.protocol.name,
                host = request.url.host,
                path = request.url.encodedPath,
                query = query,
                requestHeaders = HeaderRedaction.redact(
                    request.headers.entries().associate { it.toPair() },
                ),
                time = Clock.System.now().toEpochMilliseconds(),
                requestContentType = request.contentType()?.toString(),
                requestSize = request.contentLength(),
                replayOf = replayOf,
            )

            request.attributes.put(AlohomoraRequestKey, entity)
        }
    }

    // After the request pipeline, so the body is the serialized OutgoingContent that goes on the
    // wire. HttpSend repeats this phase for each redirect and retry; only the first is recorded,
    // matching the URL and method captured in onRequest.
    on(SendingRequest) { request, content ->
        runCatching {
            val entity = request.attributes.getOrNull(AlohomoraRequestKey) ?: return@runCatching
            if (request.attributes.contains(AlohomoraBodyCapturedKey)) return@runCatching
            request.attributes.put(AlohomoraBodyCapturedKey, Unit)
            entity.captureRequestBody(content)
        }
    }

    onResponse { response ->
        runCatching {
            val entity = response.call.request.attributes.getOrNull(AlohomoraRequestKey)
                ?: return@runCatching

            val startTime = entity.time ?: 0L
            val endTime = Clock.System.now().toEpochMilliseconds()

            entity.mockedBy = response.headers[MOCK_ID_HEADER]
            entity.status = response.status.value
            entity.message = response.status.description
            entity.responseHeaders = HeaderRedaction.redact(
                response.headers.entries().associate { it.toPair() },
            )
            entity.duration = (endTime - startTime).takeUnless { it <= 0 }
            entity.responseSize = response.contentLength()
            entity.responseContentType = response.contentType().toString()
            entity.responseBody = try {
                // Images, protobuf and the like are not read. Decoded as text they were corrupted
                // and, once JSON-escaped, inflated enough to push a snapshot past the frame cap.
                if (CapturedBody.isKnownBinary(response.contentType()?.toString())) {
                    TrafficEntry.UNABLE_PARSE_MESSAGE
                } else {
                    val raw = response.bodyAsText()
                    if (raw.looksDecodedFromBinary()) {
                        TrafficEntry.UNABLE_PARSE_MESSAGE
                    } else {
                        entity.responseBodyTruncated = raw.wasTruncatedForCapture()
                        raw.truncateForCapture()
                    }
                }
            } catch (e: Exception) {
                "<binary or unreadable body: ${e.message}>"
            }

            AlohomoraImpl.persistTrafficEntry(entity)

            // Ktor hooks are suspending, so delay() throttles the caller directly.
            // Bandwidth throttling is not implemented here — on Android, the OkHttp engine's
            // TrafficInterceptor handles it via ThrottledResponseBody; on iOS, the URLProtocol
            // handler uses dispatch_after.
            val throttle = NetworkRuleEngine.throttle
            if (throttle.latencyMs > 0) {
                delay(throttle.latencyMs.milliseconds)
            }
        }
    }
}
