package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.PartData
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable

/**
 * The request body must be read after the request pipeline has serialized it. onRequest sees
 * `setBody(dto)` as the DTO object, which was recorded as "no body", so replay sent an empty POST.
 */
class AlohomoraInspectorRequestBodyTest {

    @Serializable
    private data class Login(val user: String, val attempts: Int)

    /** Records what the engine was actually sent, so a test can prove capture did not alter it. */
    private var sentBody: ByteArray? = null

    private fun client() = HttpClient(
        MockEngine { request ->
            sentBody = request.body.toByteArray()
            respond("ok", HttpStatusCode.OK)
        },
    ) {
        install(ContentNegotiation) { json() }
        install(AlohomoraInspector)
    }

    private val HttpResponse.captured: TrafficEntry
        get() = call.request.attributes[AlohomoraRequestKey]

    @Test
    fun `serialized JSON body is captured as JSON text`() = runTest {
        val entry = client().post("https://example.com/login") {
            contentType(ContentType.Application.Json)
            setBody(Login(user = "yash", attempts = 2))
        }.captured

        assertEquals("""{"user":"yash","attempts":2}""", entry.requestBody)
        assertEquals("""{"user":"yash","attempts":2}""", sentBody?.decodeToString())
        assertTrue(entry.requestContentType.orEmpty().startsWith("application/json"))
        assertEquals(sentBody?.size?.toLong(), entry.requestSize)
        assertFalse(entry.requestBodyTruncated)
    }

    @Test
    fun `string body is captured verbatim`() = runTest {
        val entry = client().post("https://example.com/echo") {
            setBody("plain text body")
        }.captured

        assertEquals("plain text body", entry.requestBody)
    }

    @Test
    fun `UTF-8 byte array body is decoded as text`() = runTest {
        val entry = client().post("https://example.com/raw") {
            contentType(ContentType.Application.Json)
            setBody("""{"a":1}""".encodeToByteArray())
        }.captured

        assertEquals("""{"a":1}""", entry.requestBody)
        assertTrue(entry.requestContentType.orEmpty().startsWith("application/json"))
    }

    @Test
    fun `binary byte array body keeps its bytes instead of being decoded`() = runTest {
        val bytes = byteArrayOf(0x00, 0x01, 0xFF.toByte(), 0xFE.toByte())
        val entry = client().post("https://example.com/upload") {
            setBody(bytes)
        }.captured

        // The placeholder stays, so an older console renders and refuses it exactly as before.
        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, entry.requestBody)
        assertContentEquals(bytes, Base64.decode(entry.requestBodyBase64!!))
        assertTrue(sentBody.contentEquals(bytes), "Capture must not alter the bytes sent")
    }

    @Test
    fun `known binary byte array body keeps its bytes`() = runTest {
        val bytes = "GIF89a".encodeToByteArray()
        val entry = client().post("https://example.com/avatar") {
            contentType(ContentType.Image.GIF)
            setBody(bytes)
        }.captured

        assertContentEquals(bytes, Base64.decode(entry.requestBodyBase64!!))
    }

    @Test
    fun `oversized binary byte array body keeps only the placeholder`() = runTest {
        val bytes = ByteArray(1_000_001) { 0xFF.toByte() }
        val entry = client().post("https://example.com/upload") {
            setBody(bytes)
        }.captured

        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, entry.requestBody)
        assertNull(entry.requestBodyBase64)
        assertEquals(1_000_001, sentBody?.size, "The full body must still be sent")
    }

    /**
     * Why multipart stays uncaptured on Ktor. A file part's provider is the app's lambda, and one
     * that returns an already-open channel yields it once: writing the content for capture ahead
     * of the engine would send the server an empty part.
     */
    @Test
    fun `multipart with a single use part is sent whole and not captured`() = runTest {
        val fileBytes = byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte())
        val entry = client().post("https://example.com/upload") {
            setBody(singleUseMultipart(fileBytes))
        }.captured

        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, entry.requestBody)
        assertNull(entry.requestBodyBase64)
        assertTrue(sentBody!!.containsRun(fileBytes), "The engine must receive the whole part")
    }

    @Test
    fun `a single use multipart part is empty the second time it is written`() = runTest {
        val fileBytes = byteArrayOf(0x00, 0x01, 0x02, 0xFF.toByte())
        val content = singleUseMultipart(fileBytes)

        val first = content.writeToBytes()
        val second = content.writeToBytes()

        assertTrue(first.containsRun(fileBytes))
        assertFalse(second.containsRun(fileBytes), "A capture write would have taken the part")
    }

    private fun singleUseMultipart(fileBytes: ByteArray): MultiPartFormDataContent {
        val channel = ByteReadChannel(fileBytes)
        return MultiPartFormDataContent(
            parts = listOf(
                PartData.BinaryChannelItem(
                    provider = { channel },
                    partHeaders = headersOf(
                        HttpHeaders.ContentDisposition,
                        "form-data; name=\"file\"; filename=\"a.bin\"",
                    ),
                ),
            ),
            boundary = "boundary",
        )
    }

    private suspend fun OutgoingContent.WriteChannelContent.writeToBytes(): ByteArray = coroutineScope {
        val channel = ByteChannel()
        launch { writeTo(channel) }
        channel.readRemaining().readByteArray()
    }

    private fun ByteArray.containsRun(run: ByteArray): Boolean =
        (0..size - run.size).any { start -> run.indices.all { this[start + it] == run[it] } }

    @Test
    fun `GET records no body`() = runTest {
        val entry = client().get("https://example.com/items").captured

        assertNull(entry.requestBody)
    }

    @Test
    fun `oversized text body is truncated and flagged`() = runTest {
        val body = "a".repeat(1_000_001)
        val entry = client().post("https://example.com/big") {
            setBody(body)
        }.captured

        assertTrue(entry.requestBodyTruncated)
        assertTrue(entry.requestBody.orEmpty().startsWith("a".repeat(1_000_000)))
        assertEquals(1_000_001, sentBody?.size, "The full body must still be sent")
    }
}
