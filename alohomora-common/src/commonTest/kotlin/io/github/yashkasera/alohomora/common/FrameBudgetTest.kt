package io.github.yashkasera.alohomora.common

import io.github.yashkasera.alohomora.replay.ReplayBlockedReason
import io.github.yashkasera.alohomora.replay.replayBlockedReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The desktop rejects any frame over the cap, and used to reconnect into the same oversized
 * snapshot forever. These pin how bodies are shed to keep frames under it.
 */
class FrameBudgetTest {

    private fun entry(id: String, request: String? = null, response: String? = null) = TrafficEntry(
        id = id,
        url = "https://example.com/$id",
        method = "POST",
        requestBody = request,
        responseBody = response,
    )

    @Test
    fun `bodies within budget are untouched`() {
        val traffic = listOf(entry("a", "12345", "12345"), entry("b", "12345"))

        assertEquals(traffic, FrameBudget.fitBodiesToBudget(traffic, budgetChars = 15))
    }

    @Test
    fun `newest entries keep their bodies and older ones are shed`() {
        val newest = entry("new", "x".repeat(10))
        val older = entry("old", "y".repeat(10))
        val oldest = entry("oldest", "z")

        val fitted = FrameBudget.fitBodiesToBudget(listOf(newest, older, oldest), budgetChars = 15)

        assertEquals(newest, fitted[0])
        assertEquals(FrameBudget.BODY_OMITTED_MESSAGE, fitted[1].requestBody)
        // Shed too, although it would fit: once the budget is spent, older entries never jump
        // ahead of newer ones.
        assertEquals(FrameBudget.BODY_OMITTED_MESSAGE, fitted[2].requestBody)
    }

    @Test
    fun `a shed request body blocks replay instead of replaying empty`() {
        val fitted = FrameBudget.fitBodiesToBudget(listOf(entry("a", "{\"pay\":1}")), budgetChars = 0)

        assertTrue(fitted.single().requestBodyTruncated)
        assertEquals(ReplayBlockedReason.TRUNCATED_BODY, fitted.single().replayBlockedReason())
    }

    @Test
    fun `absent and empty bodies stay as they were`() {
        val fitted = FrameBudget.fitBodiesToBudget(listOf(entry("a", request = null, response = "")), 0)

        assertNull(fitted.single().requestBody)
        assertEquals("", fitted.single().responseBody)
        assertFalse(fitted.single().requestBodyTruncated)
        assertFalse(fitted.single().responseBodyTruncated)
    }

    @Test
    fun `traffic stream messages can be slimmed`() {
        val message = StreamTrafficMessage(sequence = 1, traffic = entry("a", "body", "body"))

        val slimmed = assertIs<StreamTrafficMessage>(assertNotNull(FrameBudget.withoutBodies(message)))

        assertEquals(FrameBudget.BODY_OMITTED_MESSAGE, slimmed.traffic.requestBody)
        assertEquals(FrameBudget.BODY_OMITTED_MESSAGE, slimmed.traffic.responseBody)
    }

    @Test
    fun `messages without traffic have nothing to shed`() {
        assertNull(FrameBudget.withoutBodies(AuthChallengeMessage(sequence = 1)))
    }
}
