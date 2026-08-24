package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.devtools.MOCK_ID_HEADER
import io.github.yashkasera.alohomora.devtools.NetworkRuleEngine
import io.github.yashkasera.alohomora.replay.ReplayMarker
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.delay

private val AlohomoraRequestKey = AttributeKey<TrafficEntry>("AlohomoraRequest")

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

private fun String.truncateForCapture(): String =
    if (length <= MAX_BODY_CHARS) this
    else take(MAX_BODY_CHARS) + "\n…truncated ($length chars total)"

private fun String.wasTruncatedForCapture(): Boolean = length > MAX_BODY_CHARS

@OptIn(ExperimentalUuidApi::class)
val AlohomoraInspector = createClientPlugin("AlohomoraInspector") {
    onRequest { request, content ->
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

            val rawRequestBody = when (content) {
                is String -> content
                is ByteArray -> content.decodeToString()
                else -> null
            }
            entity.requestBodyTruncated = rawRequestBody?.wasTruncatedForCapture() == true

            entity.requestBody = when {
                rawRequestBody != null -> rawRequestBody.truncateForCapture()

                // Null, not UNABLE_PARSE_MESSAGE: a GET has no body, and calling absence a
                // parse failure made every bodyless request render "Cannot parse body".
                // Checked via contentLength rather than Ktor's EmptyContent, which is not
                // resolvable across the versions this plugin supports.
                (request.contentLength() ?: 0L) == 0L -> null

                // Deliberately not content.toString(): for any streaming/multipart
                // OutgoingContent that yields a bare class name, which reads like a captured
                // body but is not one.
                else -> TrafficEntry.UNABLE_PARSE_MESSAGE
            }

            request.attributes.put(AlohomoraRequestKey, entity)
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
                val raw = response.bodyAsText()
                entity.responseBodyTruncated = raw.wasTruncatedForCapture()
                raw.truncateForCapture()
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
