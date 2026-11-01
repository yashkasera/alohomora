package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.replay.ReplayMarker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSURL
import platform.Foundation.NSURLProtocol
import platform.Foundation.setValue

/**
 * How [AlohomoraURLProtocol] decides which trace a request replays.
 *
 * The protocol used to read only the wire header and strip it on the way out, which invalidated any
 * signature the app computed over the request before it reached the session. The property is the
 * channel `urlSessionReplayHandler` uses, so it has to win; the header stays as a fallback for
 * hand-written handlers that predate it.
 */
class ReplayOfResolutionTest {

    private fun request(): NSMutableURLRequest =
        NSMutableURLRequest(uRL = NSURL(string = "https://example.com/orders"))

    @Test
    fun `the protocol property identifies a replay`() {
        val request = request()
        NSURLProtocol.setProperty("trace-1", forKey = AlohomoraReplayKeys.PROPERTY_KEY, inRequest = request)

        assertEquals("trace-1", replayOfFor(request))
    }

    @Test
    fun `the property wins over the header`() {
        val request = request()
        NSURLProtocol.setProperty("from-property", forKey = AlohomoraReplayKeys.PROPERTY_KEY, inRequest = request)
        request.setValue("from-header", forHTTPHeaderField = ReplayMarker.HEADER)

        assertEquals("from-property", replayOfFor(request))
    }

    @Test
    fun `the header is still honoured when no property is set`() {
        val request = request()
        request.setValue("from-header", forHTTPHeaderField = ReplayMarker.HEADER.lowercase())

        assertEquals("from-header", replayOfFor(request))
    }

    @Test
    fun `a blank property falls back to the header`() {
        val request = request()
        NSURLProtocol.setProperty("", forKey = AlohomoraReplayKeys.PROPERTY_KEY, inRequest = request)
        request.setValue("from-header", forHTTPHeaderField = ReplayMarker.HEADER)

        assertEquals("from-header", replayOfFor(request))
    }

    @Test
    fun `organic traffic is not a replay`() {
        assertNull(replayOfFor(request()))
    }

    @Test
    fun `the exported header name matches the marker`() {
        // The Swift-facing copy must never drift from the one capture strips.
        assertEquals(ReplayMarker.HEADER, AlohomoraReplayKeys.HEADER)
    }
}
