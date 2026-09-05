package io.github.yashkasera.alohomora.common.journey

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.propertyAt
import kotlin.time.Clock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Grades a captured session against a [JourneyDefinition]. Pure and platform-free: the same code runs
 * desktop-side today and could run on-device or in CI later with no change.
 *
 * The three-state result is the whole point (see [JourneyStatus]): a flow that never ran is
 * [JourneyStatus.NOT_EXERCISED], never a pass.
 */
object JourneyMatcher {

    /**
     * @param evaluatedAt stamped into the report; a parameter (not a `Clock` read inside) so tests are
     * deterministic. Defaults to now, matching how [Event.time] defaults.
     */
    fun evaluate(
        journey: JourneyDefinition,
        events: List<Event>,
        evaluatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    ): JourneyReport {
        // Match on time, not arrival index (a parent span ends after its children; events can arrive
        // out of send-order). Kotlin's sort is stable, so same-instant events keep input order.
        val sorted = events.sortedBy { it.time }

        val evaluation = if (journey.ordered) {
            evaluateOrdered(journey, sorted)
        } else {
            evaluateUnordered(journey, sorted)
        }

        return JourneyReport(
            journeyId = journey.id,
            name = journey.name,
            status = statusOf(evaluation),
            steps = evaluation.steps,
            timeline = evaluation.timeline,
            evaluatedAt = evaluatedAt,
        )
    }

    private class Evaluation(
        val steps: List<StepResult>,
        val timeline: List<TimelineEntry>,
        val unexpectedFailure: Boolean,
    )

    // --- Unordered: membership only. ---------------------------------------------------------------

    private fun evaluateUnordered(journey: JourneyDefinition, sorted: List<Event>): Evaluation {
        val chosen = HashMap<Int, Int>()      // step index -> the event index that satisfies it
        val repeats = HashSet<Int>()          // event indices that are extra occurrences of a step

        val steps = journey.steps.mapIndexed { stepIndex, step ->
            val hits = sorted.withIndex().filter { step.identityMatches(it.value) }
            if (hits.isEmpty()) {
                StepResult(step, StepOutcome.MISSING, null)
            } else {
                val first = hits.first()
                chosen[stepIndex] = first.index
                hits.drop(1).forEach { repeats += it.index }
                val outcome = when {
                    !step.assertionsPass(first.value) -> StepOutcome.ASSERTION_FAILED
                    hits.size > 1 && step.onRepeat == RepeatPolicy.FAIL -> StepOutcome.REPEAT_VIOLATION
                    else -> StepOutcome.MATCHED
                }
                StepResult(step, outcome, first.value.time)
            }
        }

        val chosenIndices = chosen.values.toHashSet()
        var unexpectedFailure = false
        val timeline = sorted.mapIndexed { index, event ->
            val tag = when {
                index in chosenIndices -> TimelineTag.MATCHED
                index in repeats -> TimelineTag.REPEAT
                journey.steps.any { it.identityMatches(event) } -> TimelineTag.REPEAT
                journey.allowUnexpected -> TimelineTag.NOISE
                else -> {
                    unexpectedFailure = true
                    TimelineTag.UNEXPECTED
                }
            }
            TimelineEntry(event, tag)
        }

        return Evaluation(steps, timeline, unexpectedFailure)
    }

    // --- Ordered: a cursor walks the stream. -------------------------------------------------------

    private fun evaluateOrdered(journey: JourneyDefinition, sorted: List<Event>): Evaluation {
        val results = arrayOfNulls<StepResult>(journey.steps.size)
        val timeline = ArrayList<TimelineEntry>(sorted.size)
        var cursor = 0
        var unexpectedFailure = false

        fun skipResolved() {
            while (cursor < journey.steps.size && results[cursor] != null) cursor++
        }

        for (event in sorted) {
            skipResolved()

            // 1. The next expected step fires in order.
            if (cursor < journey.steps.size && journey.steps[cursor].identityMatches(event)) {
                val step = journey.steps[cursor]
                val outcome =
                    if (step.assertionsPass(event)) StepOutcome.MATCHED else StepOutcome.ASSERTION_FAILED
                results[cursor] = StepResult(step, outcome, event.time)
                timeline += TimelineEntry(event, TimelineTag.MATCHED)
                cursor++
                continue
            }

            // 2. A later step fires early -> out of order (a hard fail, but it did match a step).
            val futureIndex = journey.steps.indices.firstOrNull {
                it > cursor && results[it] == null && journey.steps[it].identityMatches(event)
            }
            if (futureIndex != null) {
                results[futureIndex] =
                    StepResult(journey.steps[futureIndex], StepOutcome.OUT_OF_ORDER, event.time)
                timeline += TimelineEntry(event, TimelineTag.MATCHED)
                continue
            }

            // 3. An already-consumed step is seen again -> apply its repeat policy.
            val consumedIndex = journey.steps.indices.firstOrNull {
                journey.steps[it].identityMatches(event) &&
                    results[it]?.outcome.let { o -> o == StepOutcome.MATCHED || o == StepOutcome.ASSERTION_FAILED }
            }
            if (consumedIndex != null) {
                val prior = results[consumedIndex]!!
                if (journey.steps[consumedIndex].onRepeat == RepeatPolicy.FAIL) {
                    results[consumedIndex] = prior.copy(outcome = StepOutcome.REPEAT_VIOLATION)
                }
                timeline += TimelineEntry(event, TimelineTag.REPEAT)
                continue
            }

            // 4. Matches no step -> noise, ignored unless the journey forbids unexpected events.
            if (journey.allowUnexpected) {
                timeline += TimelineEntry(event, TimelineTag.NOISE)
            } else {
                unexpectedFailure = true
                timeline += TimelineEntry(event, TimelineTag.UNEXPECTED)
            }
        }

        val steps = journey.steps.mapIndexed { index, step ->
            results[index] ?: StepResult(step, StepOutcome.MISSING, null)
        }
        return Evaluation(steps, timeline, unexpectedFailure)
    }

    // --- Status ------------------------------------------------------------------------------------

    private fun statusOf(evaluation: Evaluation): JourneyStatus {
        val exercised = evaluation.steps.any { it.outcome != StepOutcome.MISSING }
        val allMatched =
            evaluation.steps.isNotEmpty() && evaluation.steps.all { it.outcome == StepOutcome.MATCHED }
        return when {
            !exercised -> JourneyStatus.NOT_EXERCISED
            allMatched && !evaluation.unexpectedFailure -> JourneyStatus.PASSED
            else -> JourneyStatus.FAILED
        }
    }

    // --- Identity + assertions ---------------------------------------------------------------------

    private fun JourneyStep.identityMatches(event: Event): Boolean =
        event.name == eventName && selector.all { (path, expected) -> event.propertyAt(path) == expected }

    private fun JourneyStep.assertionsPass(event: Event): Boolean =
        assertions.all { it.evaluate(event) }

    private fun Assertion.evaluate(event: Event): Boolean {
        val actual = event.propertyAt(path)
        return when (op) {
            AssertOp.EXISTS -> actual != null
            AssertOp.EQ -> actual != null && actual == value
            AssertOp.NEQ -> actual != value
            AssertOp.LT -> compareNumeric(actual, value) { a, b -> a < b }
            AssertOp.GT -> compareNumeric(actual, value) { a, b -> a > b }
            AssertOp.CONTAINS -> contains(actual, value)
        }
    }

    private inline fun compareNumeric(
        actual: JsonElement?,
        expected: JsonElement?,
        compare: (Double, Double) -> Boolean,
    ): Boolean {
        val a = (actual as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return false
        val b = (expected as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return false
        return compare(a, b)
    }

    private fun contains(actual: JsonElement?, expected: JsonElement?): Boolean {
        expected ?: return false
        return when (actual) {
            is JsonArray -> actual.contains(expected)
            is JsonPrimitive -> (expected as? JsonPrimitive)?.let { actual.content.contains(it.content) } ?: false
            else -> false
        }
    }
}
