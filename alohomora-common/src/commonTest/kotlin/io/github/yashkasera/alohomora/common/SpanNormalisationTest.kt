package io.github.yashkasera.alohomora.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SpanNormalisationTest {

    @Test
    fun `lowercases ids so grouping by string equality works`() {
        assertEquals("0af7651916cd43dd", normalizeSpanId("0AF7651916CD43DD"))
    }

    @Test
    fun `treats OpenTelemetry's all-zero parent sentinel as absent`() {
        // OTel reports "no parent" as 16 zeros rather than a missing field. Take it literally and no
        // span is ever a root, so a whole trace renders as orphans under a parent that never existed.
        assertNull(normalizeSpanId("0000000000000000"))
        assertNull(normalizeSpanId("00000000000000000000000000000000"))
    }

    @Test
    fun `treats blank and null as absent`() {
        assertNull(normalizeSpanId(null))
        assertNull(normalizeSpanId(""))
        assertNull(normalizeSpanId("   "))
    }

    @Test
    fun `keeps an id that merely contains zeros`() {
        assertEquals("00ff000000000001", normalizeSpanId("00FF000000000001"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals("abc123", normalizeSpanId("  ABC123 "))
    }

    @Test
    fun `null and empty attributes store SQL NULL rather than an empty object`() {
        assertNull(spanAttributesToJson(null))
        assertNull(spanAttributesToJson(emptyMap()))
    }

    @Test
    fun `attributes round-trip as string primitives`() {
        val json = spanAttributesToJson(mapOf("http.method" to "GET", "retries" to "2"))!!

        assertEquals("GET", json.jsonObject["http.method"]?.jsonPrimitive?.content)
        assertEquals("2", json.jsonObject["retries"]?.jsonPrimitive?.content)
    }

    @Test
    fun `an oversized attribute value is truncated`() {
        // No tracer caps attribute value length, so an app that puts a response body in an attribute
        // would push a frame past MAX_PAYLOAD_BYTES and kill the connection.
        val huge = "x".repeat(SPAN_ATTRIBUTE_VALUE_MAX_CHARS * 2)

        val stored = spanAttributesToJson(mapOf("body" to huge))!!
            .jsonObject["body"]!!.jsonPrimitive.content

        assertTrue(stored.length < huge.length)
        assertTrue(stored.endsWith("truncated"), "actual tail: ${stored.takeLast(20)}")
        assertTrue(stored.startsWith("x".repeat(SPAN_ATTRIBUTE_VALUE_MAX_CHARS)))
    }

    @Test
    fun `a value exactly at the limit is left alone`() {
        val exact = "y".repeat(SPAN_ATTRIBUTE_VALUE_MAX_CHARS)

        assertEquals(
            exact,
            spanAttributesToJson(mapOf("k" to exact))!!.jsonObject["k"]!!.jsonPrimitive.content,
        )
    }
}
