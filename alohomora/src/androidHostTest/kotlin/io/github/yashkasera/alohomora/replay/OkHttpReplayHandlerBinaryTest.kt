package io.github.yashkasera.alohomora.replay

import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/** `okHttpReplayHandler` sends a binary body as the bytes captured, under the captured type. */
class OkHttpReplayHandlerBinaryTest {

    @Test
    fun `a binary body is sent byte for byte with its content type`() {
        val bytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x00, 0xFF.toByte(), 0x0D, 0x0A)
        var sentBytes: ByteArray? = null
        var sentType: String? = null
        val client = OkHttpClient.Builder()
            // Stands in for the network, so the test needs no server.
            .addInterceptor { chain ->
                val body = chain.request().body!!
                sentBytes = Buffer().also(body::writeTo).readByteArray()
                sentType = body.contentType()?.toString()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody())
                    .build()
            }
            .build()
        val handler = okHttpReplayHandler(client)

        val outcome = runBlocking {
            handler.replay(
                ReplayRequest(
                    sourceTraceId = "source",
                    method = "POST",
                    url = "https://example.com/upload",
                    contentType = "multipart/form-data; boundary=b0und",
                    bodyBase64 = Base64.encode(bytes),
                ),
            )
        }

        assertIs<BinaryReplayHandler>(handler)
        assertIs<ReplayOutcome.Sent>(outcome)
        assertContentEquals(bytes, sentBytes)
        assertEquals("multipart/form-data; boundary=b0und", sentType)
    }
}
