package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.devtools.NetworkRuleEngine
import io.github.yashkasera.alohomora.replay.ReplayMarker
import io.github.yashkasera.alohomora.replay.ReplayTag
import java.net.URLDecoder
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * OkHttp interceptor that records traffic into Alohomora.
 *
 * Governing rule: **instrumentation must never change the outcome of the host app's
 * request.** `chain.proceed` is therefore called outside any try/catch of ours, every
 * capture step is individually failure-tolerant, and no exception raised by this class is
 * allowed to propagate to the caller.
 */
class TrafficInterceptor(
    private val collector: TrafficCollector = TrafficCollector(),
) : Interceptor {

    @OptIn(ExperimentalUuidApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        // A replay identifies itself with a tag, which never reaches the wire and so cannot disturb
        // a signature however this interceptor is ordered against the app's signing interceptor.
        // ReplayMarker.HEADER is honoured too, for custom handlers with no tag to set, and stripped
        // on the way out — that path does depend on ordering, which is why it is not the default.
        val original = chain.request()
        val taggedReplayOf = original.tag(ReplayTag::class.java)?.sourceTraceId
        val headerReplayOf = original.header(ReplayMarker.HEADER)
        val replayOf = taggedReplayOf ?: headerReplayOf
        val request = if (headerReplayOf == null) {
            original
        } else {
            original.newBuilder().removeHeader(ReplayMarker.HEADER).build()
        }

        // Capture is best-effort and fully isolated. Previously a malformed '%' escape in
        // URLDecoder.decode, an OOM from the unbounded body buffer, or Koin not being started
        // all surfaced to the caller as a failed HTTP request.
        val trace = runCatching {
            val body = request.body?.snapshot()
            TrafficEntry(
                id = Uuid.random().toString(),
                url = runCatching { URLDecoder.decode(request.url.toString(), "UTF-8") }
                    .getOrElse { request.url.toString() },
                method = request.method,
                scheme = request.url.scheme,
                host = request.url.host,
                path = request.url.encodedPath,
                query = request.url.query,
                requestBody = body?.text,
                time = Clock.System.now().toEpochMilliseconds(),
                requestHeaders = HeaderRedaction.redact(request.headers.toMultimap()),
                requestContentType = request.body?.contentType()?.toString(),
                requestSize = request.body?.contentLength(),
                requestBodyTruncated = body?.truncated == true,
                replayOf = replayOf,
            )
        }.onFailure { it.logCaptureFailure("request") }.getOrNull()

        trace?.let { runCatching { collector.onRequestSent(it) } }

        val mockRule = NetworkRuleEngine.findMatch(request.url.toString(), request.method)
        if (mockRule != null && trace != null) {
            runCatching {
                trace.status = mockRule.statusCode
                trace.message = "Mocked"
                trace.responseBody = mockRule.responseBody.take(MAX_BODY_BYTES.toInt())
                trace.responseContentType = mockRule.contentType
                trace.responseSize = mockRule.responseBody.length.toLong()
                trace.duration = 0
                trace.mockedBy = mockRule.id
                collector.onResponseReceived(trace)
            }.onFailure { it.logCaptureFailure("mock") }
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(mockRule.statusCode)
                .message("Mocked by Alohomora")
                .body(mockRule.responseBody.toResponseBody())
                .build()
        }

        // Outside every try/catch of ours: the host app's request and any IOException it
        // raises must reach the caller exactly as if this interceptor were absent.
        var response = chain.proceed(request)

        if (trace == null) return response

        runCatching {
            // One byte past the cap, so overflow is observed rather than inferred. Deriving it from
            // contentLength() is wrong for the common case: this runs as an application interceptor,
            // where a gzipped body has already been decompressed and its Content-Length stripped, so
            // any compressed response over the cap would be truncated and reported as complete.
            val peeked = response.peekBody(MAX_BODY_BYTES + 1).bytes()
            trace.responseBodyTruncated = peeked.size > MAX_BODY_BYTES
            trace.responseBody = peeked.decodeToString(
                startIndex = 0,
                endIndex = minOf(peeked.size.toLong(), MAX_BODY_BYTES).toInt(),
            )
            trace.status = response.code
            trace.message = response.message
            trace.duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
            trace.responseHeaders = HeaderRedaction.redact(response.headers.toMultimap())
            trace.responseContentType = response.body.contentType()?.toString()
            trace.responseSize = response.body.contentLength()
            collector.onResponseReceived(trace)
        }.onFailure { it.logCaptureFailure("response") }

        val throttle = NetworkRuleEngine.throttle
        if (throttle.latencyMs > 0) {
            runCatching { Thread.sleep(throttle.latencyMs) }
        }
        if (throttle.downloadBytesPerSec > 0) {
            runCatching {
                response = response.newBuilder()
                    .body(ThrottledResponseBody(response.body, throttle.downloadBytesPerSec))
                    .build()
            }
        }

        return response
    }

    /**
     * Reads a bounded snapshot of the request body for display.
     *
     * One-shot and duplex bodies are skipped rather than read. `writeTo` consumes a one-shot
     * body, so tracing it meant the real request was sent empty or failed outright; and a
     * duplex body's `writeTo` blocks until the response is read, deadlocking the call. This
     * mirrors what OkHttp's own HttpLoggingInterceptor does.
     */
    private fun RequestBody.snapshot(): BodySnapshot {
        if (isDuplex() || isOneShot()) return BodySnapshot(TrafficEntry.UNABLE_PARSE_MESSAGE)
        return try {
            val buffer = Buffer()
            writeTo(buffer)
            // Bounded read. The cap previously applied only to responses (via peekBody), so a
            // large multipart upload was materialised in full as a UTF-16 String and again as
            // a SQLite row.
            val size = buffer.size
            val text = buffer.readUtf8(minOf(size, MAX_BODY_BYTES))
            if (size > MAX_BODY_BYTES) {
                BodySnapshot("$text\n…truncated ($size bytes total)", truncated = true)
            } else {
                BodySnapshot(text)
            }
        } catch (e: Exception) {
            BodySnapshot(TrafficEntry.UNABLE_PARSE_MESSAGE)
        }
    }

    /**
     * A captured body plus whether it is complete.
     *
     * Carried alongside the text rather than inferred from the `…truncated` suffix, because replay
     * has to refuse a partial body and a real body may contain that text.
     */
    private data class BodySnapshot(val text: String, val truncated: Boolean = false)

    private fun Throwable.logCaptureFailure(stage: String) {
        println("[Alohomora] Failed to capture $stage trace: $message")
    }

    private companion object {
        const val MAX_BODY_BYTES = 1_000_000L
    }
}
