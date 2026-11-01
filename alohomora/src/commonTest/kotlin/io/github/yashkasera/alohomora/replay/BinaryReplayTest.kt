package io.github.yashkasera.alohomora.replay

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Binary replay, end to end on the device side: the registry only hands bytes to a handler that
 * can send them, and `ktorReplayHandler` sends exactly those bytes.
 *
 * The registry is process-global, so every test starts and ends with it empty.
 */
class BinaryReplayTest {

    private val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x00, 0xFF.toByte(), 0x0D, 0x0A)

    private fun binaryRequest(contentType: String = "image/png") = ReplayRequest(
        sourceTraceId = "source",
        method = "POST",
        url = "https://example.com/upload",
        contentType = contentType,
        bodyBase64 = Base64.encode(bytes),
    )

    @BeforeTest
    @AfterTest
    fun reset() {
        TrafficReplayRegistry.clear()
    }

    @Test
    fun `a binary body is refused for a handler that only sends text`() = runTest {
        var called = false
        TrafficReplayRegistry.register { called = true; ReplayOutcome.Sent() }

        val outcome = TrafficReplayRegistry.replay(binaryRequest())

        assertFalse(TrafficReplayRegistry.isBinarySupported)
        assertEquals(ReplayOutcome.Failed(TrafficReplayRegistry.BINARY_UNSUPPORTED), outcome)
        assertFalse(called, "The handler must not be handed a body it would send as empty")
    }

    @Test
    fun `a binary handler gets the bytes and no text body`() = runTest {
        var received: ReplayRequest? = null
        TrafficReplayRegistry.register(BinaryReplayHandler { received = it; ReplayOutcome.Sent() })

        TrafficReplayRegistry.replay(binaryRequest().copy(body = "stale text"))

        assertTrue(TrafficReplayRegistry.isBinarySupported)
        assertNull(received?.body)
        assertContentEquals(bytes, Base64.decode(received!!.bodyBase64!!))
    }

    @Test
    fun `malformed base64 is refused before any handler runs`() = runTest {
        TrafficReplayRegistry.register(BinaryReplayHandler { error("must not run") })

        val outcome = TrafficReplayRegistry.replay(binaryRequest().copy(bodyBase64 = "not base64!"))

        assertEquals(ReplayOutcome.Failed(TrafficReplayRegistry.INVALID_BINARY_BODY), outcome)
    }

    @Test
    fun `the ktor handler is binary capable and sends the exact bytes`() = runTest {
        var sentBytes: ByteArray? = null
        var sentType: String? = null
        val client = HttpClient(
            MockEngine { request ->
                sentBytes = request.body.toByteArray()
                sentType = request.body.contentType?.toString()
                respond("ok", HttpStatusCode.OK)
            },
        )
        val handler = ktorReplayHandler(client)
        TrafficReplayRegistry.register(handler)

        val outcome = TrafficReplayRegistry.replay(binaryRequest("multipart/form-data; boundary=b0und"))

        assertIs<BinaryReplayHandler>(handler)
        assertIs<ReplayOutcome.Sent>(outcome)
        assertContentEquals(bytes, sentBytes)
        // The boundary is in the content type and nowhere else; losing it orphans the body.
        assertEquals("multipart/form-data; boundary=b0und", sentType)
    }
}
