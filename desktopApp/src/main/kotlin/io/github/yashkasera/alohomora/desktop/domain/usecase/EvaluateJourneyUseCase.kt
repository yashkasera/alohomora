package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyMatcher
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

/**
 * Grades a journey against the events this device window currently holds.
 *
 * Matching is desktop-side and pure ([JourneyMatcher]); this use case just supplies the captured
 * events. Reads [DevToolsRepository.events]`.value` — the same store the Events panel renders — so a
 * report reflects exactly what the user sees.
 */
class EvaluateJourneyUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(journey: JourneyDefinition): JourneyReport =
        JourneyMatcher.evaluate(journey, repository.events.value)
}
