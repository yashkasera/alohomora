package io.github.yashkasera.alohomora.trace

import io.github.yashkasera.alohomora.common.TraceEntry
import java.net.URLDecoder
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import okhttp3.Interceptor
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer

class TraceInterceptor(
    private val collector: TraceCollector = TraceCollector(),
) : Interceptor {

    private val maxBodyBytes = 1_000_000L

    @OptIn(ExperimentalUuidApi::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        try {
            val requestBody = request.body?.toJsonStringSafe()
            val trace = TraceEntry(
                id = Uuid.random().toString(),
                url = URLDecoder.decode(request.url.toString(), "UTF-8"),
                method = request.method,
                scheme = request.url.scheme,
                host = request.url.host,
                path = request.url.encodedPath,
                query = request.url.query,
                requestBody = requestBody,
                time = Clock.System.now().toEpochMilliseconds(),
                requestHeaders = request.headers.toMultimap(),
                requestContentType = request.body?.contentType()?.toString(),
                requestSize = request.body?.contentLength()
            )
            collector.onRequestSent(trace)
            val response = chain.proceed(request)
            trace.responseBody = response.peekBody(maxBodyBytes).string()
            trace.status = response.code
            trace.message = response.message
            trace.duration = response.receivedResponseAtMillis - response.sentRequestAtMillis
            trace.responseHeaders = response.headers.toMultimap()
            trace.responseContentType = response.body.contentType()?.toString()
            trace.responseSize = response.body.contentLength()
            collector.onResponseReceived(trace)
            return response
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun RequestBody.toJsonStringSafe(): String {
        val buffer = Buffer()
        return try {
            this.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            TraceEntry.UNABLE_PARSE_MESSAGE
        }
    }
}
