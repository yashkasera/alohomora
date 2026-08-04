package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.InitialStatePayload
import io.github.yashkasera.alohomora.common.RequestClearMessage
import io.github.yashkasera.alohomora.common.RequestTraceSpansMessage
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.SpanEvent
import io.github.yashkasera.alohomora.common.StreamSpanMessage
import io.github.yashkasera.alohomora.common.TraceSpansSnapshotMessage
import io.github.yashkasera.alohomora.common.UnknownMessage
import io.github.yashkasera.alohomora.common.durationNanos
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Wire contract for span capture, and the compatibility rules that let it ship without a version
 * bump. `DevToolsProtocol.VERSION` is deliberately untouched by this feature, so the mixed-version
 * cases below are the only thing standing between that decision and a broken session.
 */
class SpanStreamProtocolTest {

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    private val sample = Span(
        id = 42,
        traceId = "0af7651916cd43dd8448eb211c80319c",
        spanId = "b7ad6b7169203331",
        parentSpanId = "00f067aa0ba902b7",
        name = "GET /users",
        kind = "CLIENT",
        startEpochNanos = 1_785_617_252_000_000_000,
        endEpochNanos = 1_785_617_252_000_400_000,
        statusCode = Span.STATUS_ERROR,
        statusDescription = "502 Bad Gateway",
        attributes = JsonObject(mapOf("http.method" to JsonPrimitive("GET"))),
        events = listOf(SpanEvent("retry", 1_785_617_252_000_200_000, mapOf("attempt" to "2"))),
        scopeName = "okhttp",
    )

    @Test
    fun `a streamed span survives the wire intact`() {
        val decoded = assertIs<StreamSpanMessage>(roundTrip(StreamSpanMessage(1, sample)))

        assertEquals(sample, decoded.span)
    }

    /**
     * The nanosecond decision under test.
     *
     * A 400 µs span is the common case for instrumented mobile code, and at millisecond storage it
     * would arrive as a zero-duration span — an invisible waterfall bar, and worse, indistinguishable
     * in ordering from every other span in the same millisecond.
     */
    @Test
    fun `a sub-millisecond span keeps its precision across the wire`() {
        val decoded = assertIs<StreamSpanMessage>(roundTrip(StreamSpanMessage(1, sample)))

        assertEquals(400_000L, decoded.span.durationNanos())
    }

    @Test
    fun `span events and attributes survive the wire`() {
        val decoded = assertIs<StreamSpanMessage>(roundTrip(StreamSpanMessage(1, sample)))

        assertEquals(1, decoded.span.events.size)
        assertEquals("retry", decoded.span.events.single().name)
        assertEquals(mapOf("attempt" to "2"), decoded.span.events.single().attributes)
        assertNotNull(decoded.span.attributes)
    }

    @Test
    fun `a span in the initial snapshot survives the wire intact`() {
        val message = InitialStateMessage(1, initialState(spans = listOf(sample)))

        val decoded = assertIs<InitialStateMessage>(roundTrip(message))
        assertEquals(listOf(sample), decoded.payload.spans)
        assertTrue(decoded.payload.spanCaptureSupported.not(), "default stays false unless set")
    }

    @Test
    fun `a whole-trace snapshot round-trips`() {
        val message = TraceSpansSnapshotMessage(1, sample.traceId, listOf(sample))

        val decoded = assertIs<TraceSpansSnapshotMessage>(roundTrip(message))
        assertEquals(sample.traceId, decoded.traceId)
        assertEquals(listOf(sample), decoded.spans)
    }

    @Test
    fun `a request for a trace's spans round-trips`() {
        val decoded = assertIs<RequestTraceSpansMessage>(
            roundTrip(RequestTraceSpansMessage(traceId = sample.traceId)),
        )

        assertEquals(sample.traceId, decoded.traceId)
    }

    /**
     * A newer desktop against an app built before span capture. The snapshot has no `spans` key and no
     * `spanCaptureSupported` key, and it has to decode — dropping the whole snapshot would take
     * traffic, events, errors, database and cache down with it, over a feature the app does not have.
     */
    @Test
    fun `a snapshot from an older app decodes with no spans and capture unsupported`() {
        val withoutSpans = """
            {
              "type": "INITIAL_STATE",
              "sequence": 1,
              "payload": {
                "events": [],
                "traffic": [],
                "databaseSchema": { "databaseName": null, "tables": [], "schemas": [] },
                "cacheKeys": []
              }
            }
        """.trimIndent()

        val decoded = assertIs<InitialStateMessage>(decodePayload(withoutSpans))

        assertEquals(emptyList(), decoded.payload.spans)
        assertFalse(
            decoded.payload.spanCaptureSupported,
            "must default to unsupported, so the desktop hides the panel rather than sending " +
                "REQUEST_TRACE_SPANS to an app that will never answer",
        )
    }

    /**
     * A span from an older app that predates a field, or from a minimal hand-written producer.
     *
     * Every field beyond the ids, name and timestamps carries a default for exactly this reason.
     */
    @Test
    fun `a minimal span decodes with defaults`() {
        val minimal = """
            {
              "type": "STREAM_SPAN",
              "sequence": 3,
              "span": {
                "traceId": "0af7651916cd43dd8448eb211c80319c",
                "spanId": "b7ad6b7169203331",
                "name": "work",
                "startEpochNanos": 100,
                "endEpochNanos": 200
              }
            }
        """.trimIndent()

        val decoded = assertIs<StreamSpanMessage>(decodePayload(minimal)).span

        assertEquals(null, decoded.parentSpanId)
        assertEquals(Span.KIND_INTERNAL, decoded.kind)
        assertEquals(Span.STATUS_UNSET, decoded.statusCode)
        assertEquals(emptyList(), decoded.events)
        assertEquals(null, decoded.attributes)
        assertFalse(decoded.isViewed)
    }

    /**
     * Stands in for an *older* desktop meeting `STREAM_SPAN` for the first time.
     *
     * It has to name a type this build genuinely does not know, since this one now knows `STREAM_SPAN`
     * and would decode it properly — the mechanism is what matters, not the name. An unknown type must
     * land on [UnknownMessage] and be ignored rather than killing the session, and that is the entire
     * basis for shipping these three message types without bumping `DevToolsProtocol.VERSION`.
     */
    @Test
    fun `an unknown stream type degrades to UnknownMessage`() {
        val decoded = decodePayload(
            """{ "type": "STREAM_SPAN_FROM_SOME_LATER_VERSION", "sequence": 7, "wat": true }""",
        )

        assertIs<UnknownMessage>(decoded)
    }

    /** An app predating span capture reads this and clears exactly what it did before. */
    @Test
    fun `a clear request from an older desktop leaves spans untouched`() {
        val decoded = assertIs<RequestClearMessage>(
            decodePayload("""{ "type": "REQUEST_CLEAR", "sequence": 0, "events": true }"""),
        )

        assertTrue(decoded.events)
        assertFalse(decoded.spans)
    }

    /**
     * The naming landmine, asserted so it cannot be quietly "tidied up".
     *
     * `RequestClearMessage.traces` means **traffic** — a misnomer predating the vocabulary rule, kept
     * because it is interop surface. Spans are cleared by `spans`. Collapsing the two would wipe the
     * wrong table, silently.
     */
    @Test
    fun `clearing spans does not clear traffic, and traces still means traffic`() {
        val decoded = assertIs<RequestClearMessage>(roundTrip(RequestClearMessage(spans = true)))

        assertTrue(decoded.spans)
        assertFalse(decoded.traces, "`traces` is traffic's flag and must stay independent of spans")
        assertFalse(decoded.events)
        assertFalse(decoded.errors)

        val traffic = assertIs<RequestClearMessage>(roundTrip(RequestClearMessage(traces = true)))
        assertTrue(traffic.traces)
        assertFalse(traffic.spans)
    }

    private fun initialState(spans: List<Span>) = InitialStatePayload(
        events = emptyList(),
        traffic = emptyList(),
        spans = spans,
        databaseSchema = DatabaseSchemaSnapshot(
            databaseName = null,
            tables = emptyList(),
            schemas = emptyList(),
        ),
        cacheKeys = emptyList(),
    )
}
