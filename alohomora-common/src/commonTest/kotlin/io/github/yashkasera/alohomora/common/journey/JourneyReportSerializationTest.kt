package io.github.yashkasera.alohomora.common.journey

import io.github.yashkasera.alohomora.common.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class JourneyReportSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a report round-trips through json unchanged`() {
        val step = JourneyStep(
            id = "s1",
            eventName = "pay",
            selector = mapOf("method" to JsonPrimitive("card")),
            assertions = listOf(Assertion("amount", AssertOp.GT, JsonPrimitive(0))),
            onRepeat = RepeatPolicy.FLAG,
        )
        val report = JourneyReport(
            journeyId = "j1",
            name = "Checkout",
            status = JourneyStatus.FAILED,
            steps = listOf(StepResult(step, StepOutcome.MATCHED, matchedEventTime = 42L)),
            timeline = listOf(
                TimelineEntry(
                    event = Event(name = "pay", properties = buildJsonObject { put("amount", 10) }, time = 42L),
                    tag = TimelineTag.MATCHED,
                ),
            ),
            evaluatedAt = 123L,
        )

        val decoded = json.decodeFromString(JourneyReport.serializer(), json.encodeToString(JourneyReport.serializer(), report))

        assertEquals(report, decoded)
    }

    @Test
    fun `a definition decodes when an unknown field is present`() {
        val encoded = """
            {
              "id": "j1",
              "name": "Checkout",
              "steps": [ { "id": "s1", "eventName": "login" } ],
              "futureField": true
            }
        """.trimIndent()

        val decoded = json.decodeFromString(JourneyDefinition.serializer(), encoded)

        assertEquals("j1", decoded.id)
        assertEquals(1, decoded.schemaVersion)
        assertEquals(false, decoded.ordered)
        assertEquals("login", decoded.steps.single().eventName)
    }
}
