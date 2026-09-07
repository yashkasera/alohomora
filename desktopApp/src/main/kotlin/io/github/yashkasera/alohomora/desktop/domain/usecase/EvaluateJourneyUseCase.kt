package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyMatcher
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    /**
     * A live report that re-grades on every change to the captured event stream — the source for the
     * validation panel's alignment view. Matching stays pure; only the input flow is live.
     */
    fun observe(journey: JourneyDefinition): Flow<JourneyReport> =
        repository.events.map { events -> JourneyMatcher.evaluate(journey, events) }
}
