package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.desktop.FakeDevToolsRepository
import io.github.yashkasera.alohomora.desktop.data.config.ConfigStoreFacade
import io.github.yashkasera.alohomora.desktop.data.config.LocalConfigStore
import io.github.yashkasera.alohomora.desktop.domain.usecase.EvaluateJourneyUseCase
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JourneyViewModelTest {

    private val baseDir = Files.createTempDirectory("alohomora-journey-vm-test").toFile()
    private val repo = FakeDevToolsRepository(events = listOf(Event(name = "a", properties = null, time = 1L)))
    private val vm = JourneyViewModel(
        configStore = ConfigStoreFacade(LocalConfigStore(baseDir)),
        evaluateJourney = EvaluateJourneyUseCase(repo),
    )

    @AfterTest
    fun tearDown() {
        vm.close()
        baseDir.deleteRecursively()
    }

    @Test
    fun `opening a new journey clears a prior report and marks it new`() {
        vm.newJourney()
        vm.editDraft { it.copy(steps = listOf(JourneyStep(id = "s1", eventName = "a"))) }
        vm.validate()
        assertNotNull(vm.uiState.value.lastReport)

        vm.newJourney()

        assertNull(vm.uiState.value.lastReport)
        assertTrue(vm.uiState.value.editorIsNew)
        assertTrue(vm.uiState.value.editorDraft?.steps.orEmpty().isEmpty())
    }
}
