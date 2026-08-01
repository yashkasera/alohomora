package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.HeaderRedaction
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.ReplayResultMessage
import io.github.yashkasera.alohomora.common.RequestReplayTraceMessage
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.replay.ReplayBlockedReason
import io.github.yashkasera.alohomora.replay.ReplayHeaderText
import io.github.yashkasera.alohomora.replay.ReplayHeaders
import io.github.yashkasera.alohomora.replay.ReplayRequest
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import io.github.yashkasera.alohomora.replay.toReplayRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire and preparation contract for trace replay.
 *
 * The desktop hands the device a fully-formed request rather than a trace id, so what these
 * messages carry decides whether an edited payload arrives intact — and what the sanitiser drops
 * decides whether the app's own interceptors get the chance to re-sign it.
 */
class TrafficReplayProtocolTest {

    @AfterTest
    fun resetStripList() {
        ReplayHeaders.additionalStripList = emptySet()
    }

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    @Test
    fun `an edited payload survives the wire byte for byte`() {
        // The entire point of the feature: the body the user typed on the desktop has to reach the
        // device unchanged, or the signature the app computes will not match what was intended.
        val body = """{"amount":4200,"note":"ünïcode ✓","nested":{"a":[1,2]}}"""
        val request = ReplayRequest(
            sourceTraceId = "trace-1",
            method = "POST",
            url = "https://api.example.com/v1/payments?dry=false",
            headers = mapOf("Accept" to listOf("application/json")),
            body = body,
            contentType = "application/json; charset=utf-8",
        )

        val decoded = roundTrip(RequestReplayTraceMessage(request = request))

        assertTrue(decoded is RequestReplayTraceMessage)
        assertEquals(request, decoded.request)
        assertEquals(body, decoded.request.body)
    }

    @Test
    fun `a failed replay reports why`() {
        // A replay can fail before any trace exists, so this is the only signal the desktop gets.
        // Losing the reason leaves the window waiting on a trace that is never coming.
        val decoded = roundTrip(
            ReplayResultMessage(
                sequence = 4,
                sourceTraceId = "trace-1",
                sent = false,
                error = "Unable to resolve host",
            ),
        ) as ReplayResultMessage

        assertFalse(decoded.sent)
        assertEquals("Unable to resolve host", decoded.error)
        assertNull(decoded.traceId)
    }

    @Test
    fun `a successful replay without a trace id is still a success`() {
        // The OkHttp handler cannot know the id the interceptor mints, so absence must not read as
        // failure.
        val decoded = roundTrip(
            ReplayResultMessage(sequence = 5, sourceTraceId = "trace-1", sent = true),
        ) as ReplayResultMessage

        assertTrue(decoded.sent)
        assertNull(decoded.traceId)
    }

    @Test
    fun `an old device reports no replay support`() {
        // Devices predating this feature omit the flag entirely. Defaulting to false is what keeps
        // the desktop from offering an action whose request would be silently dropped.
        val legacy = """
            {"type":"INITIAL_STATE","sequence":1,"payload":{"events":[],"traffic":[],
            "databaseSchema":{"tables":[],"schemas":[]},"cacheKeys":[]}}
        """.trimIndent().replace("\n", "")

        val decoded = decodePayload(legacy) as InitialStateMessage

        assertFalse(decoded.payload.replaySupported)
    }

    @Test
    fun `redacted headers are dropped rather than replayed`() {
        // Sending the literal [REDACTED] as an Authorization value produces a 401 that reads like a
        // genuine auth failure. Dropping the header is what lets the app's auth interceptor supply
        // the real one.
        val sanitized = ReplayHeaders.sanitize(
            mapOf(
                "Authorization" to listOf(HeaderRedaction.REDACTED),
                "Accept" to listOf("application/json"),
            ),
        )

        assertFalse(sanitized.containsKey("Authorization"))
        assertEquals(listOf("application/json"), sanitized["Accept"])
    }

    @Test
    fun `headers the client recomputes are dropped`() {
        // Content-Length from the captured body contradicts an edited one, and Host contradicts an
        // edited URL. Both surface as protocol errors that look nothing like their cause.
        val sanitized = ReplayHeaders.sanitize(
            mapOf(
                "Content-Length" to listOf("17"),
                "host" to listOf("api.example.com"),
                "X-Request-Id" to listOf("abc"),
            ),
        )

        assertEquals(setOf("X-Request-Id"), sanitized.keys)
    }

    @Test
    fun `an app can name its own header to regenerate`() {
        ReplayHeaders.additionalStripList = setOf("X-Payload-Signature")

        val sanitized = ReplayHeaders.sanitize(
            mapOf("x-payload-signature" to listOf("stale"), "Accept" to listOf("*/*")),
        )

        assertEquals(setOf("Accept"), sanitized.keys, "matching must be case-insensitive")
    }

    @Test
    fun `a truncated body blocks replay`() {
        // The dangerous case: truncated JSON still looks like a body, so replaying it would send
        // corrupted data and make the server look at fault.
        val trace = TrafficEntry(
            id = "t",
            url = "https://api.example.com/upload",
            method = "POST",
            requestBody = "{\"partial\":tru",
            requestBodyTruncated = true,
        )

        assertEquals(ReplayBlockedReason.TRUNCATED_BODY, trace.replayBlockedReason())
        assertNull(trace.toReplayRequest())
    }

    @Test
    fun `an unparseable body blocks replay`() {
        val trace = TrafficEntry(
            id = "t",
            url = "https://api.example.com/upload",
            method = "POST",
            requestBody = TrafficEntry.UNABLE_PARSE_MESSAGE,
        )

        assertEquals(ReplayBlockedReason.UNPARSEABLE_BODY, trace.replayBlockedReason())
        assertNull(trace.toReplayRequest())
    }

    @Test
    fun `a bodyless GET is replayable`() {
        val trace = TrafficEntry(
            id = "t",
            url = "https://api.example.com/health",
            method = "get",
            requestHeaders = mapOf("Accept" to listOf("application/json")),
        )

        assertNull(trace.replayBlockedReason())
        val request = assertNotNull(trace.toReplayRequest())
        assertEquals("GET", request.method, "the method is normalised for the client builder")
        assertEquals("t", request.sourceTraceId)
        assertNull(request.body)
    }

    @Test
    fun `a trace missing its url cannot be replayed`() {
        val trace = TrafficEntry(id = "t", method = "GET")

        assertEquals(ReplayBlockedReason.INCOMPLETE_TRACE, trace.replayBlockedReason())
    }

    @Test
    fun `headers survive a trip through the editor unchanged`() {
        // Both consoles render headers as text, let the user edit them, and parse them back. If the
        // round trip were lossy the request that goes out would differ from the one on screen.
        val headers = mapOf(
            "Accept" to listOf("application/json"),
            "X-Trace" to listOf("a", "b"),
            "X-Empty-Value" to listOf(""),
        )

        val parsed = ReplayHeaderText.parse(ReplayHeaderText.render(headers))

        assertEquals(headers, parsed)
    }

    @Test
    fun `repeated header names stay grouped and ordered`() {
        val parsed = ReplayHeaderText.parse("X-A: 1\nX-B: 2\nX-A: 3")

        assertEquals(listOf("1", "3"), parsed["X-A"])
        assertEquals(listOf("X-A", "X-B"), parsed.keys.toList(), "insertion order is preserved")
    }

    @Test
    fun `a half-typed header line is dropped rather than guessed at`() {
        // Mid-edit the text box legitimately contains junk. Inventing a header with an empty name
        // from it would send something the user never wrote.
        val parsed = ReplayHeaderText.parse("Accept: application/json\nnot-a-header\n\n: novalue\n")

        assertEquals(mapOf("Accept" to listOf("application/json")), parsed)
    }

    @Test
    fun `a header value containing a colon is preserved whole`() {
        // Splitting on every colon would truncate timestamps, URLs and IPv6 hosts.
        val parsed = ReplayHeaderText.parse("X-Origin: https://api.example.com:8443/v1")

        assertEquals(listOf("https://api.example.com:8443/v1"), parsed["X-Origin"])
    }
}
