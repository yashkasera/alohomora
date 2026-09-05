package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStatus
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class EvaluateJourneyUseCaseTest {

    private val journey = JourneyDefinition(
        id = "j1",
        name = "Login",
        steps = listOf(JourneyStep(id = "s1", eventName = "login")),
    )

    private fun event(name: String) = Event(name = name, properties = null, time = 1L)

    @Test
    fun `invoke grades the journey against the captured events`() = runTest {
        val useCase = EvaluateJourneyUseCase(FakeDevToolsRepository(events = listOf(event("login"))))

        val report = useCase(journey)

        assertEquals(JourneyStatus.PASSED, report.status)
    }

    @Test
    fun `invoke reports not exercised when the flow never ran`() = runTest {
        val useCase = EvaluateJourneyUseCase(FakeDevToolsRepository(events = listOf(event("other"))))

        assertEquals(JourneyStatus.NOT_EXERCISED, useCase(journey).status)
    }

    @Test
    fun `observe emits a report for the current event stream`() = runTest {
        val useCase = EvaluateJourneyUseCase(FakeDevToolsRepository(events = listOf(event("login"))))

        val report = useCase.observe(journey).first()

        assertEquals(JourneyStatus.PASSED, report.status)
    }
}
