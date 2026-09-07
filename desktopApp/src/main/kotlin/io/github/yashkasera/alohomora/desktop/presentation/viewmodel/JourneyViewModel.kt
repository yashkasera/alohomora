package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.journey.Assertion
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.common.journey.RepeatPolicy
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore
import io.github.yashkasera.alohomora.desktop.domain.usecase.EvaluateJourneyUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.JourneyUiState
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the journey list, editor, and validation surfaces.
 *
 * One immutable [JourneyUiState] as a [StateFlow]; all mutation happens here. The editor "Save" is a
 * debounced write (500 ms, cancel-and-relaunch) mirroring [NetworkRulesViewModel.scheduleAutoSave] —
 * a working-tree write, never a commit, so per-keystroke edits do not thrash the store.
 *
 * LOCAL scope only in Milestone 1; the store facade routes TEAM once a repo is connected (Phase 2).
 */
class JourneyViewModel(
    private val configStore: ConfigStore,
    private val evaluateJourney: EvaluateJourneyUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var saveJob: Job? = null
    private var liveJob: Job? = null

    private val _uiState = MutableStateFlow(JourneyUiState())
    val uiState: StateFlow<JourneyUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = configStore.list(ConfigKind.Journeys, _uiState.value.scope)
            _uiState.update { it.copy(journeys = items, isLoading = false) }
        }
    }

    fun onScopeChange(scope: ConfigScope) {
        _uiState.update { it.copy(scope = scope) }
        refresh()
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    fun select(id: String?) = _uiState.update { it.copy(selectedId = id, lastReport = null) }

    @OptIn(ExperimentalUuidApi::class)
    fun newJourney() {
        val draft = JourneyDefinition(
            id = Uuid.random().toString(),
            name = "New journey",
            steps = emptyList(),
        )
        // Opening a fresh journey must not inherit a prior journey's report or live run.
        liveJob?.cancel()
        liveJob = null
        _uiState.update {
            it.copy(
                editorDraft = draft,
                selectedId = draft.id,
                editorIsNew = true,
                lastReport = null,
                liveJourneyId = null,
                liveJourneyName = "",
            )
        }
    }

    fun editExisting(id: String) {
        val existing = _uiState.value.journeys.firstOrNull { it.value.id == id }?.value ?: return
        liveJob?.cancel()
        liveJob = null
        _uiState.update {
            it.copy(
                editorDraft = existing,
                selectedId = id,
                editorIsNew = false,
                lastReport = null,
                liveJourneyId = null,
                liveJourneyName = "",
            )
        }
    }

    fun closeEditor() = _uiState.update { it.copy(editorDraft = null) }

    /**
     * Applies a pure edit to the open draft and schedules a debounced save. The transform keeps the
     * business shape (validation, id stability) out of composables.
     */
    fun editDraft(transform: (JourneyDefinition) -> JourneyDefinition) {
        val current = _uiState.value.editorDraft ?: return
        val next = transform(current)
        _uiState.update { it.copy(editorDraft = next) }
        scheduleSave(next)
        // If the live panel is grading this journey, re-run against the edited definition — otherwise it
        // keeps grading the snapshot captured when Live was clicked.
        if (_uiState.value.liveJourneyId == next.id) startLiveValidation(next)
    }

    fun promoteEventToStep(eventName: String) = editDraft { draft ->
        draft.copy(
            steps = draft.steps + JourneyStep(id = newStepId(), eventName = eventName),
        )
    }

    /** Applies a pure edit to one step of the open draft, preserving the others' order. */
    fun editStep(stepId: String, transform: (JourneyStep) -> JourneyStep) = editDraft { draft ->
        draft.copy(steps = draft.steps.map { if (it.id == stepId) transform(it) else it })
    }

    fun removeStep(stepId: String) = editDraft { draft ->
        draft.copy(steps = draft.steps.filterNot { it.id == stepId })
    }

    fun setStepSelector(stepId: String, selector: Map<String, kotlinx.serialization.json.JsonElement>) =
        editStep(stepId) { it.copy(selector = selector) }

    fun setStepAssertions(stepId: String, assertions: List<Assertion>) =
        editStep(stepId) { it.copy(assertions = assertions) }

    fun setStepOnRepeat(stepId: String, policy: RepeatPolicy) =
        editStep(stepId) { it.copy(onRepeat = policy) }

    fun deleteJourney(id: String) {
        scope.launch {
            configStore.deleteLocal(ConfigKind.Journeys, id)
            _uiState.update {
                it.copy(
                    selectedId = if (it.selectedId == id) null else it.selectedId,
                    editorDraft = if (it.editorDraft?.id == id) null else it.editorDraft,
                )
            }
            refresh()
        }
    }

    /** Grades the open draft (or the selected saved journey) against the captured events. */
    fun validate() {
        val journey = _uiState.value.editorDraft ?: _uiState.value.selected?.value ?: return
        val report = evaluateJourney(journey)
        _uiState.update { it.copy(lastReport = report) }
    }

    /**
     * Opens the full-width live validation panel, re-grading [journey] on every event that streams in.
     * Grades the current draft when one is open, else the given journey.
     */
    fun startLiveValidation(journey: JourneyDefinition) {
        liveJob?.cancel()
        _uiState.update { it.copy(liveJourneyId = journey.id, liveJourneyName = journey.name) }
        liveJob = scope.launch {
            evaluateJourney.observe(journey).collect { report ->
                _uiState.update { it.copy(lastReport = report) }
            }
        }
    }

    fun stopLiveValidation() {
        liveJob?.cancel()
        liveJob = null
        _uiState.update { it.copy(liveJourneyId = null, liveJourneyName = "") }
    }

    /**
     * Proposes a journey to the team: the one explicit branch + push + create-proposal action. Surfaces
     * the returned proposal (for the In-review link) or a message on failure — e.g. no repo connected.
     */
    fun share(journeyId: String) {
        val item = _uiState.value.journeys.firstOrNull { it.value.id == journeyId }?.value
            ?: _uiState.value.editorDraft?.takeIf { it.id == journeyId }
            ?: return
        scope.launch {
            runCatching { configStore.shareWithTeam(ConfigKind.Journeys, item) }
                .onSuccess { proposal -> _uiState.update { it.copy(lastProposal = proposal, message = null) } }
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Share failed") } }
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private fun scheduleSave(draft: JourneyDefinition) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE)
            configStore.saveLocal(ConfigKind.Journeys, draft)
            refresh()
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun newStepId(): String = Uuid.random().toString()

    fun close() {
        liveJob?.cancel()
        // Flush a pending debounced save so the last edit is not lost on window close.
        val draft = _uiState.value.editorDraft
        if (draft != null && saveJob?.isActive == true) {
            saveJob?.cancel()
            kotlinx.coroutines.runBlocking { configStore.saveLocal(ConfigKind.Journeys, draft) }
        }
        scope.cancel()
    }

    private companion object {
        val SAVE_DEBOUNCE = 500.milliseconds
    }
}
