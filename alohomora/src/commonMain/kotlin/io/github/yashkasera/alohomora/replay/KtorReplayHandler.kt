package io.github.yashkasera.alohomora.replay

import io.github.yashkasera.alohomora.network.AlohomoraReplayOfKey
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType

/**
 * A [TraceReplayHandler] that re-sends a captured request through [client].
 *
 * The point is that the call goes through the same [HttpClient] the app uses, so every plugin on it
 * runs again — a signing plugin recomputes the signature over the body being replayed rather than the
 * one that was captured. Editing a payload in the console and getting a valid signature back is a
 * consequence of that, not something this function does.
 *
 * Works on Android and iOS alike, since the client is Ktor's and not a platform type. Pass the client
 * the app uses for the endpoint being replayed: one without the signing plugin produces
 * authentic-looking 401s.
 *
 * ```kotlin
 * Alohomora.registerReplayHandler(ktorReplayHandler(myHttpClient))
 * ```
 *
 * @param client the app's own client, complete with its plugins
 */
fun ktorReplayHandler(client: HttpClient): TraceReplayHandler = TraceReplayHandler { replay ->
    try {
        client.request(replay.url) {
            // Trimmed here as well as in the editors: HttpMethod.parse throws on "POST " and a
            // ReplayRequest can also be built by a caller that never went through a form.
            method = HttpMethod.parse(replay.method.trim().uppercase())

            // Content-Type is set once, through contentType(), because header() appends: forwarding
            // the captured one *and* setting it would send two and let the engine pick.
            replay.headers
                .filterKeys { !it.equals(CONTENT_TYPE, ignoreCase = true) }
                .forEach { (name, values) -> values.forEach { header(name, it) } }

            replay.resolvedContentType()?.let { contentType(ContentType.parse(it)) }

            // An attribute, not a header: AlohomoraInspector reads it to link the resulting trace
            // back to its source, and nothing reaches the wire for a signing plugin to sign.
            attributes.put(AlohomoraReplayOfKey, replay.sourceTraceId)

            replay.body?.let { setBody(it) }
        }

        // No id to hand back: the inspector mints its own and stamps replayOf with sourceTraceId,
        // which is how the console finds the resulting trace.
        ReplayOutcome.Sent()
    } catch (e: Exception) {
        // A non-2xx is a real answer and shows up as one in the trace the inspector recorded. Only a
        // transport failure — or an unparseable hand-edited URL — lands here.
        ReplayOutcome.Failed(e.message ?: e::class.simpleName ?: "Replay failed")
    }
}

/** The explicit content type, falling back to whatever the captured headers carried. */
private fun ReplayRequest.resolvedContentType(): String? = contentType
    ?: headers.entries.firstOrNull { it.key.equals(CONTENT_TYPE, ignoreCase = true) }
        ?.value
        ?.firstOrNull()

private const val CONTENT_TYPE = "Content-Type"
