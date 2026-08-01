package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.AlohomoraInternal
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TraceEntry
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsText
import io.ktor.http.contentLength
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.util.AttributeKey
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val AlohomoraRequestKey = AttributeKey<TraceEntry>("AlohomoraRequest")

/** Upper bound on a captured body, matching the OkHttp interceptor. */
private const val MAX_BODY_CHARS = 1_000_000

private fun String.truncateForCapture(): String =
    if (length <= MAX_BODY_CHARS) this
    else take(MAX_BODY_CHARS) + "\n…truncated ($length chars total)"

@OptIn(ExperimentalUuidApi::class)
val AlohomoraInspector = createClientPlugin("AlohomoraInspector") {
    onRequest { request, content ->
        // Wrapped: a debug tool must not be able to fail the host app's request.
        runCatching {
            val query = request.url.parameters.entries()
                .joinToString("&") { "${it.key}=${it.value.joinToString(",")}" }
                .takeIf { it.isNotEmpty() }

            val entity = TraceEntry(
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
            )

            entity.requestBody = when {
                content is String -> content.truncateForCapture()
                content is ByteArray -> content.decodeToString().truncateForCapture()

                // Null, not UNABLE_PARSE_MESSAGE: a GET has no body, and calling absence a
                // parse failure made every bodyless request render "Cannot parse body".
                // Checked via contentLength rather than Ktor's EmptyContent, which is not
                // resolvable across the versions this plugin supports.
                (request.contentLength() ?: 0L) == 0L -> null

                // Deliberately not content.toString(): for any streaming/multipart
                // OutgoingContent that yields a bare class name, which reads like a captured
                // body but is not one.
                else -> TraceEntry.UNABLE_PARSE_MESSAGE
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

            entity.status = response.status.value
            entity.message = response.status.description
            entity.responseHeaders = HeaderRedaction.redact(
                response.headers.entries().associate { it.toPair() },
            )
            entity.duration = (endTime - startTime).takeUnless { it <= 0 }
            entity.responseSize = response.contentLength()
            entity.responseContentType = response.contentType().toString()
            entity.responseBody = try {
                response.bodyAsText().truncateForCapture()
            } catch (e: Exception) {
                "<binary or unreadable body: ${e.message}>"
            }

            AlohomoraInternal.recordTrace(entity)
        }
    }
}
