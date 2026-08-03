package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.InitialStateMessage
import io.github.yashkasera.alohomora.common.RequestClearMessage
import io.github.yashkasera.alohomora.common.StreamErrorMessage
import io.github.yashkasera.alohomora.common.UnknownMessage
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.desktop.data.local.ErrorStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Wire contract for error capture, and the compatibility rules that let it ship without a version
 * bump. `DevToolsProtocol.VERSION` is deliberately untouched by this feature — see its KDoc — so the
 * mixed-version cases below are the only thing standing between that decision and a broken session.
 */
class ErrorStreamProtocolTest {

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    private val sample = Error(
        id = 42,
        place = "thread main",
        reason = "IllegalStateException: cart was empty",
        stackTrace = "java.lang.IllegalStateException: cart was empty\n\tat com.example.Cart.check(Cart.kt:17)",
        time = 1_785_617_252_000,
    )

    @Test
    fun `a streamed error survives the wire intact`() {
        val decoded = assertIs<StreamErrorMessage>(roundTrip(StreamErrorMessage(1, sample)))

        assertEquals(sample, decoded.error)
        // The trace is the whole reason this message exists; the mobile list query projects it away.
        assertTrue(decoded.error.stackTrace!!.contains("Cart.kt:17"))
    }

    @Test
    fun `an error in the initial snapshot survives the wire intact`() {
        val message = InitialStateMessage(
            sequence = 1,
            payload = initialState(errors = listOf(sample)),
        )

        val decoded = assertIs<InitialStateMessage>(roundTrip(message))
        assertEquals(listOf(sample), decoded.payload.errors)
    }

    /**
     * A newer desktop against an app built before error capture. The snapshot has no `errors` key at
     * all, and it has to decode — dropping the whole snapshot would take traffic, events, database
     * and cache down with it, over a feature the app simply does not have.
     */
    @Test
    fun `a snapshot from an older app decodes with no errors`() {
        val withoutErrors = """
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

        val decoded = assertIs<InitialStateMessage>(decodePayload(withoutErrors))
        assertEquals(emptyList(), decoded.payload.errors)
    }

    /**
     * Stands in for an *older* desktop meeting `STREAM_ERROR` for the first time.
     *
     * It has to use a type this build genuinely does not know, since this one now knows
     * `STREAM_ERROR` and would decode it properly — the mechanism is what matters, not the name.
     * An unknown type must land on [UnknownMessage] and be ignored rather than killing the session,
     * and that is the entire basis for shipping `STREAM_ERROR` without bumping
     * `DevToolsProtocol.VERSION`.
     */
    @Test
    fun `an unknown stream type degrades to UnknownMessage`() {
        val decoded = decodePayload(
            """{ "type": "STREAM_SOMETHING_NOT_INVENTED_YET", "sequence": 7, "wat": true }""",
        )

        assertIs<UnknownMessage>(decoded)
    }

    /** An app predating error capture reads this and clears exactly what it did before. */
    @Test
    fun `a clear request from an older desktop leaves errors untouched`() {
        val decoded = assertIs<RequestClearMessage>(
            decodePayload("""{ "type": "REQUEST_CLEAR", "sequence": 0, "events": true }"""),
        )

        assertTrue(decoded.events)
        assertEquals(false, decoded.errors)
    }

    @Test
    fun `clearing errors round-trips`() {
        val decoded = assertIs<RequestClearMessage>(roundTrip(RequestClearMessage(errors = true)))

        assertTrue(decoded.errors)
        assertEquals(false, decoded.traces)
        assertEquals(false, decoded.events)
    }

    @Test
    fun `the store keeps errors newest-first and ignores redelivery`() {
        val store = ErrorStore()
        store.replace(listOf(sample.copy(id = 1), sample.copy(id = 3), sample.copy(id = 2)))

        assertEquals(listOf(3L, 2L, 1L), store.errors.value.map { it.id })

        // The device reseeds its stream adapter on every snapshot, so the same row can arrive twice.
        store.append(sample.copy(id = 3))
        assertEquals(listOf(3L, 2L, 1L), store.errors.value.map { it.id })

        store.append(sample.copy(id = 4))
        assertEquals(listOf(4L, 3L, 2L, 1L), store.errors.value.map { it.id })
    }

    @Test
    fun `the desktop reads the same exception title as the device console`() {
        // One shared parser in alohomora-common; two consoles disagreeing on the row title was the
        // reason it moved there.
        assertEquals("IllegalStateException", sample.exceptionTypeName())
    }

    private fun initialState(errors: List<Error>) =
        io.github.yashkasera.alohomora.common.InitialStatePayload(
            events = emptyList(),
            traffic = emptyList(),
            errors = errors,
            databaseSchema = io.github.yashkasera.alohomora.common.DatabaseSchemaSnapshot(
                databaseName = null,
                tables = emptyList(),
                schemas = emptyList(),
            ),
            cacheKeys = emptyList(),
        )
}
