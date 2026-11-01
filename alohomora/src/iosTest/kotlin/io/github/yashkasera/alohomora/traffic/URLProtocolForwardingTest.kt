package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.common.CapturedBody
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.CapturedRequestBody
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create

/**
 * The pieces of [AlohomoraURLProtocol]'s forwarding that run without a loading system.
 *
 * What these cannot reach, and stays review-and-device only: that the client actually forwards a
 * challenge to the app's session delegate, that a reported redirect re-enters the protocol, and that
 * callbacks land on the client thread.
 */
class URLProtocolForwardingTest {

    // region ClientCallbackGate

    @Test
    fun `nothing reaches the client after stopLoading`() {
        val gate = ClientCallbackGate()
        gate.stop()

        assertFalse(gate.isOpen)
        assertFalse(gate.claimTerminal(), "a cancelled task's didCompleteWithError must not fail the load")
    }

    @Test
    fun `the terminal callback is delivered exactly once`() {
        val gate = ClientCallbackGate()

        assertTrue(gate.claimTerminal())
        assertFalse(gate.claimTerminal(), "the cancellation after a redirect must not be reported as well")
        assertFalse(gate.isOpen, "no data after the load has finished")
    }

    @Test
    fun `a stop after the redirect report is still observed`() {
        val gate = ClientCallbackGate()
        assertTrue(gate.claimTerminal())
        assertFalse(gate.isStopped)

        gate.stop()

        assertTrue(gate.isStopped, "the redirect's trailing didFailWithError must be suppressed")
    }

    @Test
    fun `callbacks flow until the load ends`() {
        assertTrue(ClientCallbackGate().isOpen)
    }

    // endregion

    // region PendingChallenges

    @Test
    fun `an answer completes the handler of the challenge it names`() {
        val pending = PendingChallenges<String, String>()
        pending.add("a", "handler-a")
        pending.add("b", "handler-b")

        assertEquals("handler-b", pending.take("b"))
        assertEquals("handler-a", pending.take("a"))
    }

    @Test
    fun `a challenge is answered once`() {
        val pending = PendingChallenges<String, String>()
        pending.add("a", "handler-a")

        assertEquals("handler-a", pending.take("a"))
        assertNull(pending.take("a"), "a second answer must not call the completion handler again")
    }

    @Test
    fun `an unrecognised challenge answers the oldest pending one rather than hanging`() {
        val pending = PendingChallenges<String, String>()
        pending.add("ours", "handler")

        assertEquals("handler", pending.take("a copy made by the loading system"))
    }

    @Test
    fun `stopLoading drains every unanswered challenge`() {
        val pending = PendingChallenges<String, String>()
        pending.add("a", "handler-a")
        pending.add("b", "handler-b")

        assertEquals(listOf("handler-a", "handler-b"), pending.drain())
        assertNull(pending.take("a"))
    }

    @Test
    fun `continuing without a credential is UseCredential not default handling`() {
        assertEquals(NSURLSessionAuthChallengeUseCredential, ChallengeReply.CONTINUE_WITHOUT_CREDENTIAL.disposition())
        assertEquals(NSURLSessionAuthChallengeUseCredential, ChallengeReply.USE_CREDENTIAL.disposition())
        assertEquals(NSURLSessionAuthChallengePerformDefaultHandling, ChallengeReply.PERFORM_DEFAULT_HANDLING.disposition())
        assertEquals(NSURLSessionAuthChallengeCancelAuthenticationChallenge, ChallengeReply.CANCEL.disposition())
    }

    // endregion

    // region CappedBodyCapture

    @Test
    fun `a body under the cap is stored whole`() {
        val capture = CappedBodyCapture(maxBytes = 16)
        feed(capture, """{"ok":true}""".encodeToByteArray())

        assertEquals(CapturedResponseBody("""{"ok":true}""", truncated = false), capture.result())
        assertEquals(11L, capture.totalBytes)
    }

    @Test
    fun `only the first maxBytes plus one bytes are retained`() {
        val capture = CappedBodyCapture(maxBytes = 4)

        assertEquals(5, capture.bytesWanted())
        feed(capture, "abc".encodeToByteArray())
        assertEquals(2, capture.bytesWanted())
        feed(capture, "defgh".encodeToByteArray())
        assertEquals(0, capture.bytesWanted(), "later chunks must not be copied at all")
        feed(capture, "ijklmnop".encodeToByteArray())

        assertEquals(CapturedResponseBody("abcd", truncated = true), capture.result())
        assertEquals(16L, capture.totalBytes, "the stored size is what the client received")
    }

    @Test
    fun `a body of exactly maxBytes is not truncated`() {
        val capture = CappedBodyCapture(maxBytes = 4)
        feed(capture, "abcd".encodeToByteArray())

        assertEquals(CapturedResponseBody("abcd", truncated = false), capture.result())
    }

    @Test
    fun `a binary body gets the unparseable placeholder rather than null`() {
        val capture = CappedBodyCapture(maxBytes = 64)
        feed(capture, byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x00, 0x1A))

        assertEquals(CapturedResponseBody(TrafficEntry.UNABLE_PARSE_MESSAGE, truncated = false), capture.result())
    }

    @Test
    fun `a skipped body keeps counting bytes but retains none`() {
        val capture = CappedBodyCapture(maxBytes = 64)
        capture.skip(CapturedBody.STREAMED_PLACEHOLDER)

        assertEquals(0, capture.bytesWanted())
        feed(capture, "data: tick\n\n".encodeToByteArray())

        assertEquals(CapturedResponseBody(CapturedBody.STREAMED_PLACEHOLDER, truncated = false), capture.result())
        assertEquals(12L, capture.totalBytes)
    }

    @Test
    fun `event streams and binary types are never decoded`() {
        assertEquals(CapturedBody.STREAMED_PLACEHOLDER, CappedBodyCapture.placeholderFor("text/event-stream"))
        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, CappedBodyCapture.placeholderFor("image/png"))
        assertNull(CappedBodyCapture.placeholderFor("application/json"))
        assertNull(CappedBodyCapture.placeholderFor(null))
    }

    @Test
    fun `a binary request body keeps its exact bytes`() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0xFF.toByte())

        val captured = capturedRequestBody(png.toNSData(), maxBytes = 64, contentType = null)

        assertEquals(TrafficEntry.UNABLE_PARSE_MESSAGE, captured.text)
        assertContentEquals(png, Base64.decode(captured.base64!!))
    }

    @Test
    fun `a binary request body over the cap keeps only the placeholder`() {
        val captured = capturedRequestBody(ByteArray(10) { 0xFF.toByte() }.toNSData(), maxBytes = 4, contentType = null)

        assertEquals(CapturedRequestBody(TrafficEntry.UNABLE_PARSE_MESSAGE), captured)
    }

    @Test
    fun `a text request body stays text and an empty one is empty`() {
        assertEquals(
            CapturedRequestBody("""{"a":1}"""),
            capturedRequestBody("""{"a":1}""".encodeToByteArray().toNSData(), maxBytes = 64, contentType = "application/json"),
        )
        assertEquals(CapturedRequestBody(""), capturedRequestBody(NSData(), maxBytes = 64, contentType = null))
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned {
        NSData.create(bytes = it.addressOf(0), length = size.toULong())
    }

    /** Feeds [bytes] the way the protocol does: the whole chunk counted, only the wanted prefix copied. */
    private fun feed(capture: CappedBodyCapture, bytes: ByteArray) {
        val wanted = minOf(capture.bytesWanted(), bytes.size)
        capture.append(bytes.copyOf(wanted), bytes.size.toLong())
    }

    // endregion

    // region trafficOutcome

    @Test
    fun `an HTTP response is stored with its status`() {
        assertEquals(TrafficOutcome(404, "not found"), trafficOutcome(404, "not found", transportError = null))
    }

    @Test
    fun `a transport failure has no status`() {
        // -999, -1001 and -1009 used to be stored as if a server had answered with them.
        assertEquals(TrafficOutcome(null, "cancelled"), trafficOutcome(null, null, transportError = "cancelled"))
    }

    @Test
    fun `a failure mid-body keeps the server status and reports the failure`() {
        assertEquals(
            TrafficOutcome(200, "The network connection was lost."),
            trafficOutcome(200, "no error", transportError = "The network connection was lost."),
        )
    }

    // endregion
}
