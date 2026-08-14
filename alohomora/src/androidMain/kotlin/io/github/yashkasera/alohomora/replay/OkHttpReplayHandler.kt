package io.github.yashkasera.alohomora.replay

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * A [TrafficReplayHandler] that re-sends a captured request through [client].
 *
 * The whole point is `client.newCall(...)`: it re-runs the app's full interceptor chain, so a
 * signing interceptor recomputes the signature over the body being replayed rather than the one
 * that was captured. Editing a payload in the console and getting a valid signature back is a
 * consequence of that, not something this function does itself. Pass the same client the app uses
 * for the endpoint being replayed — a client without the signing interceptor produces
 * authentic-looking 401s.
 *
 * ```kotlin
 * Alohomora.registerReplayHandler(okHttpReplayHandler(retrofitOkHttpClient))
 * ```
 *
 * Interceptor order does not matter here: the replay marker travels in a [ReplayTag], not a header,
 * so nothing this adds ends up inside a signature.
 *
 * @param client the app's own client, complete with its interceptors
 */
@Suppress("unused")
fun okHttpReplayHandler(client: OkHttpClient): TrafficReplayHandler =
    TrafficReplayHandler { request ->
        withContext(Dispatchers.IO) {
            try {
                // Closed rather than read: TrafficInterceptor already peeked the body for capture, and
                // the console renders the replay from the trace it recorded. A non-2xx is a real answer
                // from the server and shows up as one — only a transport failure is a replay failure.
                client.newCall(request.toOkHttpRequest()).execute().close()

                // No id to hand back: the interceptor mints its own, and stamps replayOf with
                // sourceTraceId, which is how the console finds the resulting trace.
                ReplayOutcome.Sent()
            } catch (e: Exception) {
                ReplayOutcome.Failed(e.message ?: e::class.java.simpleName)
            }
        }
    }

private fun ReplayRequest.toOkHttpRequest(): Request {
    val mediaType = contentType?.toMediaTypeOrNull()

    // Built from the string body rather than reusing the captured RequestBody, which was consumed
    // at capture time and may have been edited since.
    //
    // Encoded to bytes here rather than via String.toRequestBody, which rewrites a charset-less
    // media type: `application/json` goes out as `application/json; charset=utf-8`. Any signing
    // scheme that canonicalises Content-Type into the signed set would then reject every replay,
    // and the request would differ from the captured one for no reason the user can see.
    val capturedBody = body
    val requestBody = when {
        capturedBody != null -> capturedBody.encodeToByteArray().toRequestBody(mediaType)

        // OkHttp requires a body for these and rejects one for GET/HEAD, so an empty body is the
        // only correct stand-in when a POST was captured with none.
        method in METHODS_REQUIRING_BODY -> ByteArray(0).toRequestBody(mediaType)

        else -> null
    }

    return Request.Builder()
        .url(url)
        .method(method, requestBody)
        .apply {
            headers.forEach { (name, values) -> values.forEach { addHeader(name, it) } }
            // A tag, not a header: TrafficInterceptor reads it to link the new trace back to its
            // source, and nothing goes on the wire that the app's signing interceptor could sign.
            tag(ReplayTag::class.java, ReplayTag(sourceTraceId))
        }
        .build()
}

private val METHODS_REQUIRING_BODY = setOf("POST", "PUT", "PATCH", "PROPPATCH", "REPORT")
