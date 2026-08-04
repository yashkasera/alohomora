package io.github.yashkasera.alohomora.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.encodeToJsonElement

/**
 * The shared properties formatter.
 *
 * Shared rather than duplicated because the JsonNull guard was previously on one side only: the mobile
 * row documented the trap while the mobile detail sheet fell into it and rendered the word "null".
 */
class EventPropertiesTest {

    private fun event(properties: JsonElement?) =
        Event(id = 1, name = "App.Start", properties = properties, time = 1_000)

    @Test
    fun `properties render as indented json`() {
        val element = Json.encodeToJsonElement(mapOf("screen" to "Checkout"))

        val rendered = event(element).prettyProperties()

        assertTrue(rendered.contains("\n"), "rendered compact rather than indented: $rendered")
        assertTrue(rendered.contains("\"screen\""), rendered)
        assertTrue(rendered.contains("\"Checkout\""), rendered)
    }

    @Test
    fun `a null properties value renders as an empty object`() {
        assertEquals("{}", event(null).prettyProperties())
    }

    @Test
    fun `a JsonNull properties value renders as an empty object`() {
        // recordEvent encodes a null property map through encodeToJsonElement which yields JsonNull —
        // not Kotlin null — so `?: "{}"` never fired and the word "null" reached the screen.
        assertEquals("{}", event(JsonNull).prettyProperties())
    }

    @Test
    fun `an empty property map renders as an empty object`() {
        val element = Json.encodeToJsonElement(emptyMap<String, String>())

        assertEquals("{}", event(element).prettyProperties())
    }

    @Test
    fun `a nested object keeps its structure`() {
        val element = Json.parseToJsonElement("""{"user":{"id":"7"}}""")

        val rendered = event(element).prettyProperties()

        assertEquals(element, Json.parseToJsonElement(rendered))
    }
}
