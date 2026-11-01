package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.common.CapturedBody
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeStringUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.runTest

/**
 * The response hook may only read a body it can read without taking it from the app: a saved one.
 * Reading a streaming call's body threw DoubleReceiveException in the app, and reading an event
 * stream suspended the call for as long as the server kept it open.
 */
class AlohomoraInspectorStreamingResponseTest {

    private fun client(engine: MockEngine) = HttpClient(engine) { install(AlohomoraInspector) }

    private val HttpResponse.captured: TrafficEntry
        get() = call.request.attributes[AlohomoraRequestKey]

    @Test
    fun `saved response body is captured`() = runTest {
        val client = client(MockEngine { respond("""{"ok":true}""", HttpStatusCode.OK) })

        val entry = client.get("https://example.com/items").captured

        assertEquals("""{"ok":true}""", entry.responseBody)
    }

    @Test
    fun `streaming call leaves the body to the app`() = runTest {
        val client = client(MockEngine { respond("streamed payload", HttpStatusCode.OK) })

        var entry: TrafficEntry? = null
        val text = client.prepareGet("https://example.com/download").execute { response ->
            entry = response.captured
            response.bodyAsText()
        }

        assertEquals("streamed payload", text)
        assertEquals(CapturedBody.STREAMED_PLACEHOLDER, entry?.responseBody)
    }

    @Test
    fun `event stream reaches the app while it is still open`() = runTest(timeout = 10.seconds) {
        // Never closed by the server side, like a live feed.
        val stream = ByteChannel()
        stream.writeStringUtf8("data: first\n\n")
        stream.flush()
        val client = client(
            MockEngine {
                respond(
                    stream,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, ContentType.Text.EventStream.toString()),
                )
            },
        )

        var entry: TrafficEntry? = null
        val firstLine = client.prepareGet("https://example.com/events").execute { response ->
            entry = response.captured
            response.bodyAsChannel().readUTF8Line()
        }
        stream.close()

        assertEquals("data: first", firstLine)
        assertEquals(CapturedBody.STREAMED_PLACEHOLDER, entry?.responseBody)
    }
}
