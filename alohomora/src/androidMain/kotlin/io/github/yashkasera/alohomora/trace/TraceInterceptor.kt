package io.github.yashkasera.alohomora.trace

import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.TraceEntry
import java.net.URLDecoder
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

/**
 * OkHttp interceptor that records traffic into Alohomora.
 *
 * Governing rule: **instrumentation must never change the outcome of the host app's
 * request.** `chain.proceed` is therefore called outside any try/catch of ours, every
 * capture step is individually failure-tolerant, and no exception raised by this class is
 * allowed to propagate to the caller.
 */
class TraceInterceptor(
    private val collector: TraceCollector = TraceCollector(),
) : Interceptor {

    @OptIn(ExperimentalUuidApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Capture is best-effort and fully isolated. Previously a malformed '%' escape in
        // URLDecoder.decode, an OOM from the unbounded body buffer, or Koin not being started
        // all surfaced to the caller as a failed HTTP request.
        val trace = runCatching {
            TraceEntry(
                id = Uuid.random().toString(),
                url = runCatching { URLDecoder.decode(request.url.toString(), "UTF-8") }
                    .getOrElse { request.url.toString() },
                method = request.method,
                scheme = request.url.scheme,
                host = request.url.host,
                path = request.url.encodedPath,
                query = request.url.query,
                requestBody = request.body?.snapshot(),
                time = Clock.System.now().toEpochMilliseconds(),
                requestHeaders = HeaderRedaction.redact(request.headers.toMultimap()),
                requestContentType = request.body?.contentType()?.toString(),
                requestSize = request.body?.contentLength(),
            )
        }.onFailure { it.logCaptureFailure("request") }.getOrNull()

        trace?.let { runCatching { collector.onRequestSent(it) } }

        // Outside every try/catch of ours: the host app's request and any IOException it
        // raises must reach the caller exactly as if this interceptor were absent.
        val response = chain.proceed(request)

        if (trace == null) return response

        runCatching {
            trace.responseBody = response.peekBody(MAX_BODY_BYTES).string()
            trace.status = response.code
            trace.message = response.message
            trace.duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
            trace.responseHeaders = HeaderRedaction.redact(response.headers.toMultimap())
            trace.responseContentType = response.body.contentType()?.toString()
            trace.responseSize = response.body.contentLength()
            collector.onResponseReceived(trace)
        }.onFailure { it.logCaptureFailure("response") }

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
    private fun RequestBody.snapshot(): String {
        if (isDuplex() || isOneShot()) return TraceEntry.UNABLE_PARSE_MESSAGE
        return try {
            val buffer = Buffer()
            writeTo(buffer)
            // Bounded read. The cap previously applied only to responses (via peekBody), so a
            // large multipart upload was materialised in full as a UTF-16 String and again as
            // a SQLite row.
            val size = buffer.size
            val text = buffer.readUtf8(minOf(size, MAX_BODY_BYTES))
            if (size > MAX_BODY_BYTES) "$text\n…truncated ($size bytes total)" else text
        } catch (e: Exception) {
            TraceEntry.UNABLE_PARSE_MESSAGE
        }
    }

    private fun Throwable.logCaptureFailure(stage: String) {
        println("[Alohomora] Failed to capture $stage trace: $message")
    }

    private companion object {
        const val MAX_BODY_BYTES = 1_000_000L
    }
}
