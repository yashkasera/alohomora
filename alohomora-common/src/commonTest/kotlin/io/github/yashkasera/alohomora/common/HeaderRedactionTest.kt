package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.common.HeaderRedaction.REDACTED
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * The redaction policy: defaults on, the app's own names layered over them, and an opt-out that
 * touches only the defaults.
 *
 * [HeaderRedaction] is process-global, so every test starts from the shipped state rather than
 * whatever the previous test configured.
 */
class HeaderRedactionTest {

    @BeforeTest
    fun reset() {
        HeaderRedaction.clearHeaders()
        HeaderRedaction.setDefaultsEnabled(true)
    }

    private val captured = mapOf(
        "Authorization" to listOf("Bearer secret"),
        "Proxy-Authorization" to listOf("Basic abc"),
        "Cookie" to listOf("session=1"),
        "Set-Cookie" to listOf("a=1", "b=2"),
        "X-Api-Key" to listOf("key"),
        "X-Session" to listOf("s-1"),
        "Accept" to listOf("application/json"),
    )

    @Test
    fun `defaults redact credentials out of the box`() {
        val redacted = HeaderRedaction.redact(captured)!!

        HeaderRedaction.DEFAULT_HEADERS.forEach { lower ->
            val name = captured.keys.single { it.lowercase() == lower }
            assertEquals(captured.getValue(name).map { REDACTED }, redacted[name], name)
        }
        assertEquals(listOf("s-1"), redacted["X-Session"], "not a default")
        assertEquals(listOf("application/json"), redacted["Accept"])
    }

    @Test
    fun `every value of a repeated header is redacted`() {
        val redacted = HeaderRedaction.redact(captured)!!

        assertEquals(listOf(REDACTED, REDACTED), redacted["Set-Cookie"])
    }

    @Test
    fun `matching ignores case`() {
        val redacted = HeaderRedaction.redact(
            mapOf("AUTHORIZATION" to listOf("t"), "cookie" to listOf("c"), "x-API-key" to listOf("k")),
        )!!

        assertEquals(setOf(listOf(REDACTED)), redacted.values.toSet())
    }

    @Test
    fun `custom names are matched case-insensitively`() {
        HeaderRedaction.setHeaders(setOf("X-SESSION"))

        assertEquals(listOf(REDACTED), HeaderRedaction.redact(captured)!!["X-Session"])
    }

    @Test
    fun `opting out disables only the defaults`() {
        HeaderRedaction.setHeaders(setOf("X-Session"))
        HeaderRedaction.setDefaultsEnabled(false)

        val redacted = HeaderRedaction.redact(captured)!!

        assertEquals(listOf("Bearer secret"), redacted["Authorization"])
        assertEquals(listOf("session=1"), redacted["Cookie"])
        assertEquals(listOf(REDACTED), redacted["X-Session"], "the app's own names still apply")
    }

    @Test
    fun `nothing is redacted with defaults off and no custom names`() {
        HeaderRedaction.setDefaultsEnabled(false)

        assertSame(captured, HeaderRedaction.redact(captured), "untouched maps are returned as-is")
    }

    @Test
    fun `defaults and custom names combine`() {
        HeaderRedaction.setHeaders(setOf("X-Session"))

        assertEquals(
            HeaderRedaction.DEFAULT_HEADERS + "x-session",
            HeaderRedaction.headersToRedact,
        )
        val redacted = HeaderRedaction.redact(captured)!!
        assertEquals(listOf(REDACTED), redacted["Authorization"])
        assertEquals(listOf(REDACTED), redacted["X-Session"])
        assertEquals(listOf("application/json"), redacted["Accept"])
    }

    @Test
    fun `clearing custom names keeps the defaults`() {
        HeaderRedaction.setHeaders(setOf("X-Session"))
        HeaderRedaction.clearHeaders()

        assertEquals(HeaderRedaction.DEFAULT_HEADERS, HeaderRedaction.headersToRedact)
    }

    @Test
    fun `setting custom names replaces the previous custom names`() {
        HeaderRedaction.setHeaders(setOf("X-Session"))
        HeaderRedaction.setHeaders(setOf("X-Tenant"))

        assertEquals(HeaderRedaction.DEFAULT_HEADERS + "x-tenant", HeaderRedaction.headersToRedact)
    }

    @Test
    fun `re-enabling the defaults keeps the custom names`() {
        HeaderRedaction.setHeaders(setOf("X-Session"))
        HeaderRedaction.setDefaultsEnabled(false)
        HeaderRedaction.setDefaultsEnabled(true)

        assertEquals(HeaderRedaction.DEFAULT_HEADERS + "x-session", HeaderRedaction.headersToRedact)
    }

    @Test
    fun `null headers stay null`() {
        assertEquals(null, HeaderRedaction.redact(null))
    }
}
