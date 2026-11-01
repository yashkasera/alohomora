package io.github.yashkasera.alohomora.traffic

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer

/**
 * An event stream must reach the app event by event. peekBody blocks until it has 1 MB or the
 * stream ends, and a live event stream does neither, so an EventSource behind TrafficInterceptor
 * saw nothing at all.
 */
class TrafficInterceptorEventStreamTest {

    /** Holds the stream open after its first event, the way a live feed does. */
    private val endOfStream = CountDownLatch(1)
    private val executor = Executors.newSingleThreadExecutor()

    @AfterTest
    fun tearDown() {
        // Releases the call thread if the interceptor did block on the stream.
        endOfStream.countDown()
        executor.shutdownNow()
    }

    private inner class OpenEventStream : Source {
        private var sentFirst = false

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (!sentFirst) {
                sentFirst = true
                val event = Buffer().writeUtf8("data: first\n\n")
                val size = event.size
                sink.writeAll(event)
                return size
            }
            endOfStream.await()
            return -1
        }

        override fun timeout(): Timeout = Timeout.NONE

        override fun close() = Unit
    }

    @Test
    fun `first event reaches the app while the stream is still open`() {
        val client = OkHttpClient.Builder()
            .addInterceptor(TrafficInterceptor(TrafficCollector(showNotification = false)))
            // Stands in for the network, so the test needs no server.
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(OpenEventStream().buffer().asResponseBody("text/event-stream".toMediaType()))
                    .build()
            }
            .build()

        val firstLine = executor.submit<String?> {
            val request = Request.Builder().url("https://example.com/events").build()
            client.newCall(request).execute().use { it.body.source().readUtf8Line() }
        }

        assertEquals("data: first", firstLine.get(5, TimeUnit.SECONDS))
    }
}
