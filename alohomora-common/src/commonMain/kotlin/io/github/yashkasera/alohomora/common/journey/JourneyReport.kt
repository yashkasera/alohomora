package io.github.yashkasera.alohomora.common.journey

import io.github.yashkasera.alohomora.common.Event
import kotlinx.serialization.Serializable

/**
 * The result of grading a session against a [JourneyDefinition]. `@Serializable` because it feeds the
 * JSON reporter and the MCP `verify_journey` tool directly.
 */
@Serializable
data class JourneyReport(
    val journeyId: String,
    val name: String,
    val status: JourneyStatus,
    val steps: List<StepResult>,
    /** Every actual event, tagged — the raw material for the annotated timeline view. */
    val timeline: List<TimelineEntry>,
    val evaluatedAt: Long,
)

@Serializable
data class StepResult(
    val step: JourneyStep,
    val outcome: StepOutcome,
    /** Time (ms) of the event that satisfied the step, or `null` when nothing did. */
    val matchedEventTime: Long? = null,
)

enum class StepOutcome { MATCHED, MISSING, OUT_OF_ORDER, REPEAT_VIOLATION, ASSERTION_FAILED }

@Serializable
data class TimelineEntry(val event: Event, val tag: TimelineTag)

enum class TimelineTag { MATCHED, NOISE, REPEAT, UNEXPECTED }

/**
 * Three states, and the guardrail against a false green:
 * - [PASSED]: the flow ran and every expected step matched.
 * - [FAILED]: the flow ran but something is missing / out of order / a failed assertion / an
 *   unexpected event when [JourneyDefinition.allowUnexpected] is off.
 * - [NOT_EXERCISED]: no step's event ever fired. The flow never ran. Never a pass.
 */
enum class JourneyStatus { PASSED, FAILED, NOT_EXERCISED }
