package io.github.yashkasera.alohomora.replay

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * A binary body is replayable only when the app's handler can send bytes, and then only as bytes:
 * the placeholder text beside it must never reach the wire.
 */
class BinaryReplayPreparationTest {

    private fun binaryEntry() = TrafficEntry(
        id = "a",
        url = "https://example.com/upload",
        method = "post",
        requestBody = TrafficEntry.UNABLE_PARSE_MESSAGE,
        requestBodyBase64 = "iVBORw0KGgo=",
        requestContentType = "image/png",
    )

    @Test
    fun `a binary body is refused when the handler cannot send bytes`() {
        assertEquals(ReplayBlockedReason.BINARY_REPLAY_UNSUPPORTED, binaryEntry().replayBlockedReason())
        assertNull(binaryEntry().toReplayRequest())
    }

    @Test
    fun `a binary body replays as bytes when the handler can send them`() {
        assertNull(binaryEntry().replayBlockedReason(binaryReplaySupported = true))

        val request = assertNotNull(binaryEntry().toReplayRequest(binaryReplaySupported = true))

        assertEquals("iVBORw0KGgo=", request.bodyBase64)
        assertNull(request.body, "The placeholder must not be sent as a text body")
        assertEquals("image/png", request.contentType)
        assertEquals("POST", request.method)
    }

    @Test
    fun `a text body is unaffected by binary support`() {
        val text = binaryEntry().copy(requestBody = """{"a":1}""", requestBodyBase64 = null)

        assertEquals(text.toReplayRequest(), text.toReplayRequest(binaryReplaySupported = true))
        assertNull(assertNotNull(text.toReplayRequest()).bodyBase64)
    }

    @Test
    fun `a placeholder without bytes stays unparseable whatever the handler`() {
        // An entry from an app that predates binary capture, or a body over the cap.
        val old = binaryEntry().copy(requestBodyBase64 = null)

        assertEquals(ReplayBlockedReason.UNPARSEABLE_BODY, old.replayBlockedReason(binaryReplaySupported = true))
    }

    @Test
    fun `a truncated entry is not replayable even with bytes beside it`() {
        val truncated = binaryEntry().copy(requestBodyTruncated = true)

        assertEquals(ReplayBlockedReason.UNPARSEABLE_BODY, truncated.replayBlockedReason(binaryReplaySupported = true))
        assertNull(truncated.toReplayRequest(binaryReplaySupported = true))
    }
}
