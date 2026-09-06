package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.validateExamples
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope
import io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore
import io.github.yashkasera.alohomora.desktop.presentation.model.DeepLinkCatalogUiState
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Drives the deep-link catalog: viewer, typed editor, and the share/sync affordances. Mirrors
 * [JourneyViewModel] — one immutable [DeepLinkCatalogUiState], debounced local save, validation kept in
 * step with edits so a self-contradicting definition surfaces before it is shared.
 */
class DeepLinkCatalogViewModel(
    private val configStore: ConfigStore,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var saveJob: Job? = null

    private val _uiState = MutableStateFlow(DeepLinkCatalogUiState())
    val uiState: StateFlow<DeepLinkCatalogUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = configStore.list(ConfigKind.DeepLinks, _uiState.value.scope)
            _uiState.update { it.copy(defs = items, isLoading = false) }
        }
    }

    fun onScopeChange(scope: ConfigScope) {
        _uiState.update { it.copy(scope = scope) }
        refresh()
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    fun select(id: String?) = _uiState.update { it.copy(selectedId = id) }

    @OptIn(ExperimentalUuidApi::class)
    fun newDef() {
        val draft = DeepLinkDef(
            id = Uuid.random().toString(),
            name = "New deep link",
            module = "general",
            uriTemplate = "",
        )
        _uiState.update { it.copy(editorDraft = draft, selectedId = draft.id, validationErrors = emptyList()) }
    }

    fun editExisting(id: String) {
        val existing = _uiState.value.defs.firstOrNull { it.value.id == id }?.value ?: return
        _uiState.update {
            it.copy(editorDraft = existing, selectedId = id, validationErrors = existing.validateExamples())
        }
    }

    fun closeEditor() = _uiState.update { it.copy(editorDraft = null) }

    fun editDraft(transform: (DeepLinkDef) -> DeepLinkDef) {
        val current = _uiState.value.editorDraft ?: return
        val next = transform(current)
        _uiState.update { it.copy(editorDraft = next, validationErrors = next.validateExamples()) }
        scheduleSave(next)
    }

    fun deleteDef(id: String) {
        scope.launch {
            configStore.deleteLocal(ConfigKind.DeepLinks, id)
            _uiState.update {
                it.copy(
                    selectedId = if (it.selectedId == id) null else it.selectedId,
                    editorDraft = if (it.editorDraft?.id == id) null else it.editorDraft,
                )
            }
            refresh()
        }
    }

    fun share(id: String) {
        val item = _uiState.value.defs.firstOrNull { it.value.id == id }?.value
            ?: _uiState.value.editorDraft?.takeIf { it.id == id }
            ?: return
        scope.launch {
            runCatching { configStore.shareWithTeam(ConfigKind.DeepLinks, item) }
                .onSuccess { proposal -> _uiState.update { it.copy(lastProposal = proposal, message = null) } }
                .onFailure { e -> _uiState.update { it.copy(message = e.message ?: "Share failed") } }
        }
    }

    private fun scheduleSave(draft: DeepLinkDef) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DEBOUNCE)
            configStore.saveLocal(ConfigKind.DeepLinks, draft)
            refresh()
        }
    }

    fun close() {
        val draft = _uiState.value.editorDraft
        if (draft != null && saveJob?.isActive == true) {
            saveJob?.cancel()
            runBlocking { configStore.saveLocal(ConfigKind.DeepLinks, draft) }
        }
        scope.cancel()
    }

    private companion object {
        val SAVE_DEBOUNCE = 500.milliseconds
    }
}
