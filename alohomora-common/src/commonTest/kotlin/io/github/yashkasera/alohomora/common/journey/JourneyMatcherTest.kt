package io.github.yashkasera.alohomora.common.journey

import io.github.yashkasera.alohomora.common.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class JourneyMatcherTest {

    private fun event(name: String, time: Long, properties: JsonObject? = null) =
        Event(name = name, properties = properties, time = time)

    private fun step(
        name: String,
        selector: Map<String, JsonPrimitive> = emptyMap(),
        assertions: List<Assertion> = emptyList(),
        onRepeat: RepeatPolicy = RepeatPolicy.IGNORE,
    ) = JourneyStep(
        id = "step-$name",
        eventName = name,
        selector = selector,
        assertions = assertions,
        onRepeat = onRepeat,
    )

    private fun journey(
        ordered: Boolean = false,
        allowUnexpected: Boolean = true,
        steps: List<JourneyStep>,
    ) = JourneyDefinition(
        id = "j1",
        name = "Checkout",
        ordered = ordered,
        allowUnexpected = allowUnexpected,
        steps = steps,
    )

    private fun evaluate(journey: JourneyDefinition, events: List<Event>) =
        JourneyMatcher.evaluate(journey, events, evaluatedAt = 0L)

    // --- three states ------------------------------------------------------------------------------

    @Test
    fun `unordered journey passes when every step is present`() {
        val report = evaluate(
            journey(steps = listOf(step("login"), step("pay"))),
            listOf(event("login", 1), event("browse", 2), event("pay", 3)),
        )

        assertEquals(JourneyStatus.PASSED, report.status)
        assertTrue(report.steps.all { it.outcome == StepOutcome.MATCHED })
    }

    @Test
    fun `a missing step fails the journey`() {
        val report = evaluate(
            journey(steps = listOf(step("login"), step("pay"))),
            listOf(event("login", 1)),
        )

        assertEquals(JourneyStatus.FAILED, report.status)
        assertEquals(StepOutcome.MISSING, report.steps.single { it.step.eventName == "pay" }.outcome)
    }

    @Test
    fun `a flow that never ran is not exercised rather than failed`() {
        val report = evaluate(
            journey(steps = listOf(step("login"), step("pay"))),
            listOf(event("something-else", 1), event("noise", 2)),
        )

        assertEquals(JourneyStatus.NOT_EXERCISED, report.status)
    }

    @Test
    fun `an empty session is not exercised`() {
        val report = evaluate(journey(steps = listOf(step("login"))), emptyList())
        assertEquals(JourneyStatus.NOT_EXERCISED, report.status)
    }

    // --- selector vs assertion ---------------------------------------------------------------------

    @Test
    fun `selector picks the occurrence with the matching property`() {
        val steps = listOf(step("screen_view", selector = mapOf("name" to JsonPrimitive("cart"))))
        val report = evaluate(
            journey(steps = steps),
            listOf(
                event("screen_view", 1, buildJsonObject { put("name", "home") }),
                event("screen_view", 2, buildJsonObject { put("name", "cart") }),
            ),
        )

        assertEquals(JourneyStatus.PASSED, report.status)
        assertEquals(2L, report.steps.single().matchedEventTime)
    }

    @Test
    fun `a name match with a wrong selector does not exercise the step`() {
        val steps = listOf(step("screen_view", selector = mapOf("name" to JsonPrimitive("cart"))))
        val report = evaluate(
            journey(steps = steps),
            listOf(event("screen_view", 1, buildJsonObject { put("name", "home") })),
        )

        assertEquals(JourneyStatus.NOT_EXERCISED, report.status)
    }

    @Test
    fun `an assertion failure on a matched event is a hard fail`() {
        val steps = listOf(
            step("pay", assertions = listOf(Assertion("amount", AssertOp.GT, JsonPrimitive(0)))),
        )
        val report = evaluate(
            journey(steps = steps),
            listOf(event("pay", 1, buildJsonObject { put("amount", 0) })),
        )

        assertEquals(JourneyStatus.FAILED, report.status)
        assertEquals(StepOutcome.ASSERTION_FAILED, report.steps.single().outcome)
    }

    @Test
    fun `assertion operators evaluate against nested properties`() {
        val steps = listOf(
            step(
                "checkout",
                assertions = listOf(
                    Assertion("user.tier", AssertOp.EQ, JsonPrimitive("gold")),
                    Assertion("user.age", AssertOp.LT, JsonPrimitive(100)),
                    Assertion("items", AssertOp.CONTAINS, JsonPrimitive("sku-1")),
                    Assertion("coupon", AssertOp.EXISTS),
                ),
            ),
        )
        val props = buildJsonObject {
            put("coupon", "SAVE10")
            put("user", buildJsonObject { put("tier", "gold"); put("age", 30) })
            put("items", kotlinx.serialization.json.buildJsonArray { add(JsonPrimitive("sku-1")) })
        }
        val report = evaluate(journey(steps = steps), listOf(event("checkout", 1, props)))

        assertEquals(JourneyStatus.PASSED, report.status)
    }

    // --- ordered mode ------------------------------------------------------------------------------

    @Test
    fun `ordered journey passes in the right sequence`() {
        val report = evaluate(
            journey(ordered = true, steps = listOf(step("a"), step("b"), step("c"))),
            listOf(event("a", 1), event("b", 2), event("c", 3)),
        )
        assertEquals(JourneyStatus.PASSED, report.status)
    }

    @Test
    fun `ordered journey flags a step that fires early`() {
        val report = evaluate(
            journey(ordered = true, steps = listOf(step("a"), step("b"), step("c"))),
            listOf(event("a", 1), event("c", 2), event("b", 3)),
        )

        assertEquals(JourneyStatus.FAILED, report.status)
        assertEquals(StepOutcome.OUT_OF_ORDER, report.steps.single { it.step.eventName == "c" }.outcome)
    }

    @Test
    fun `order is judged by event time not arrival order`() {
        val report = evaluate(
            journey(ordered = true, steps = listOf(step("a"), step("b"))),
            // Supplied out of time order; the matcher sorts by time first.
            listOf(event("b", 20), event("a", 10)),
        )
        assertEquals(JourneyStatus.PASSED, report.status)
    }

    // --- repeats -----------------------------------------------------------------------------------

    @Test
    fun `a repeated step fails when onRepeat is FAIL`() {
        val report = evaluate(
            journey(ordered = true, steps = listOf(step("a", onRepeat = RepeatPolicy.FAIL), step("b"))),
            listOf(event("a", 1), event("b", 2), event("a", 3)),
        )

        assertEquals(JourneyStatus.FAILED, report.status)
        assertEquals(StepOutcome.REPEAT_VIOLATION, report.steps.single { it.step.eventName == "a" }.outcome)
    }

    @Test
    fun `a repeated step is tolerated when onRepeat is IGNORE`() {
        val report = evaluate(
            journey(ordered = true, steps = listOf(step("a", onRepeat = RepeatPolicy.IGNORE), step("b"))),
            listOf(event("a", 1), event("b", 2), event("a", 3)),
        )

        assertEquals(JourneyStatus.PASSED, report.status)
        assertEquals(TimelineTag.REPEAT, report.timeline.last().tag)
    }

    // --- unexpected events -------------------------------------------------------------------------

    @Test
    fun `an unexpected event is ignored by default`() {
        val report = evaluate(
            journey(steps = listOf(step("a"))),
            listOf(event("a", 1), event("surprise", 2)),
        )

        assertEquals(JourneyStatus.PASSED, report.status)
        assertEquals(TimelineTag.NOISE, report.timeline.single { it.event.name == "surprise" }.tag)
    }

    @Test
    fun `an unexpected event fails the journey when allowUnexpected is off`() {
        val report = evaluate(
            journey(allowUnexpected = false, steps = listOf(step("a"))),
            listOf(event("a", 1), event("surprise", 2)),
        )

        assertEquals(JourneyStatus.FAILED, report.status)
        assertEquals(TimelineTag.UNEXPECTED, report.timeline.single { it.event.name == "surprise" }.tag)
    }

    // --- timeline ----------------------------------------------------------------------------------

    @Test
    fun `the timeline tags every event exactly once`() {
        val events = listOf(event("a", 1), event("noise", 2), event("b", 3))
        val report = evaluate(journey(steps = listOf(step("a"), step("b"))), events)

        assertEquals(events.size, report.timeline.size)
        assertEquals(2, report.timeline.count { it.tag == TimelineTag.MATCHED })
    }
}
