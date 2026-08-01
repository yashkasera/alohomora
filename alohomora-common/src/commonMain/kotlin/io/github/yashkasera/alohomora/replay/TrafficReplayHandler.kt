package io.github.yashkasera.alohomora.replay

import kotlinx.serialization.Serializable

/**
 * A captured request, prepared for re-sending through the host app's own HTTP client.
 *
 * Deliberately not a `TrafficEntry`. A traffic entry is a *record* of what happened and is not safe to
 * put back on the wire: sensitive headers were replaced with `[REDACTED]` at capture time, and
 * `Content-Length` describes the body that was sent rather than the one being replayed. The
 * headers here have already had both classes of value removed, so a handler can pass this
 * straight to its client and let the app's own interceptor chain re-derive auth and signatures.
 *
 * @property sourceTraceId the trace this replay was built from, for linking the two in the console
 * @property body the request body as text; null for a bodyless request
 */
@Serializable
data class ReplayRequest(
    val sourceTraceId: String,
    val method: String,
    val url: String,
    val headers: Map<String, List<String>> = emptyMap(),
    val body: String? = null,
    val contentType: String? = null,
)

/** The result of handing a [ReplayRequest] to a [TrafficReplayHandler]. */
sealed interface ReplayOutcome {

    /**
     * The request reached the server and a response came back.
     *
     * [traceId] is the id of the trace the app's interceptor recorded for the replay, when the
     * handler was able to determine it. Null is not a failure — it only means the console has
     * to show the replay as a newly arrived trace rather than linking it to its source.
     */
    data class Sent(val traceId: String? = null) : ReplayOutcome

    /** The request could not be executed, or the client raised before a response arrived. */
    data class Failed(val reason: String) : ReplayOutcome
}

/**
 * Executes a replayed request using the host app's own HTTP client.
 *
 * Alohomora never sends the request itself. Anything the app derives per-request — payload
 * signatures, bearer tokens, certificate pinning — lives in the app's client chain, so a
 * request assembled and sent by the library would carry a signature computed over the
 * *original* body and be rejected the moment the body is edited. Re-entering the app's chain
 * is what makes an edited payload get a matching signature.
 *
 * Register one at startup:
 * ```kotlin
 * Alohomora.registerReplayHandler(okHttpReplayHandler(myOkHttpClient))
 * ```
 */
fun interface TrafficReplayHandler {
    suspend fun replay(request: ReplayRequest): ReplayOutcome
}
