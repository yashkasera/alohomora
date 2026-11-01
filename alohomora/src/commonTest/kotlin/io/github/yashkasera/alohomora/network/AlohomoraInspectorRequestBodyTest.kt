package io.github.yashkasera.alohomora.network

import io.github.yashkasera.alohomora.common.TrafficEntry
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
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
    fun `binary byte array body is refused not decoded`() = runTest {
        val bytes = byteArrayOf(0x00, 0x01, 0xFF.toByte(), 0xFE.toByte())
        val entry = client().post("https://example.com/upload") {
            setBody(bytes)
        }.captured

        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, entry.requestBody)
        assertTrue(sentBody.contentEquals(bytes), "Capture must not alter the bytes sent")
    }

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
