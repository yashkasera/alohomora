package io.github.yashkasera.alohomora.desktop.data.config

import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.common.journey.JourneyStatus
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.common.journey.StepOutcome
import io.github.yashkasera.alohomora.common.journey.StepResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JourneyReportersTest {

    private fun step(name: String) = JourneyStep(id = "s-$name", eventName = name)

    private val report = JourneyReport(
        journeyId = "j1",
        name = "Checkout",
        status = JourneyStatus.FAILED,
        steps = listOf(
            StepResult(step("login"), StepOutcome.MATCHED, 1L),
            StepResult(step("pay"), StepOutcome.MISSING, null),
        ),
        timeline = emptyList(),
        evaluatedAt = 0L,
    )

    @Test
    fun `junit xml reports one failure for the missing step`() {
        val xml = JUnitJourneyReporter.render(report)
        assertTrue(xml.contains("""tests="2""""))
        assertTrue(xml.contains("""failures="1""""))
        assertTrue(xml.contains("<failure message=\"MISSING\""))
    }

    @Test
    fun `json round-trips to an equal report`() {
        val rendered = JsonJourneyReporter.render(report)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(JourneyReport.serializer(), rendered)
        assertEquals(report, decoded)
    }

    @Test
    fun `html includes the status and step names`() {
        val html = HtmlJourneyReporter.render(report)
        assertTrue(html.contains("FAILED"))
        assertTrue(html.contains("login"))
        assertTrue(html.contains("pay"))
    }
}
