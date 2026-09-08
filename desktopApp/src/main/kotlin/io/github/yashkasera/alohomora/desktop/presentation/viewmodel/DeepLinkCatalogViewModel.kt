package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.validateStructure
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

    /** Query and the module jump-chip are two modes of narrowing; typing clears any chip. */
    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query, moduleFilter = null) }

    /** Selecting a module chip clears the query; passing null returns to "All". */
    fun onModuleFilterChange(module: String?) =
        _uiState.update { it.copy(moduleFilter = module, query = "") }

    fun select(id: String?) = _uiState.update { it.copy(selectedId = id) }

    @OptIn(ExperimentalUuidApi::class)
    fun newDef() {
        // Seeded empty on purpose: the draft opens invalid, so required fields are flagged and it
        // cannot persist until the user fills name/module/template.
        val draft = DeepLinkDef(
            id = Uuid.random().toString(),
            name = "",
            module = "",
            uriTemplate = "",
        )
        _uiState.update {
            it.copy(
                editorDraft = draft,
                selectedId = draft.id,
                fieldErrors = draft.validateStructure(),
                validationErrors = emptyList(),
            )
        }
    }

    /**
     * Promotes a concrete URL (from the deep-link runner) into a catalogued definition: the URL becomes
     * the template and the first example, name/module are guessed from the path, and the editor opens so
     * the user can refine it (add typed params) and then Share it with the team.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun createFromUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        val afterScheme = trimmed.substringAfter("://", trimmed)
        val pathPart = afterScheme.substringBefore('?').trimEnd('/')
        val segments = pathPart.split('/').filter { it.isNotBlank() }
        val def = DeepLinkDef(
            id = Uuid.random().toString(),
            name = segments.lastOrNull() ?: "",
            module = segments.firstOrNull() ?: "",
            uriTemplate = trimmed,
            examples = listOf(trimmed),
        )
        val errors = def.validateStructure()
        _uiState.update {
            it.copy(
                scope = ConfigScope.LOCAL,
                editorDraft = def,
                selectedId = def.id,
                fieldErrors = errors,
                validationErrors = errors.warnings,
            )
        }
        // Only persist a well-formed guess; a bare/path-less URL opens the editor for the user to
        // complete name/module rather than silently seeding the catalog.
        if (errors.isValid) {
            scope.launch {
                configStore.saveLocal(ConfigKind.DeepLinks, def)
                refresh()
            }
        }
    }

    fun editExisting(id: String) {
        val existing = _uiState.value.defs.firstOrNull { it.value.id == id }?.value ?: return
        val errors = existing.validateStructure()
        _uiState.update {
            it.copy(editorDraft = existing, selectedId = id, fieldErrors = errors, validationErrors = errors.warnings)
        }
    }

    fun closeEditor() = _uiState.update { it.copy(editorDraft = null) }

    fun editDraft(transform: (DeepLinkDef) -> DeepLinkDef) {
        val current = _uiState.value.editorDraft ?: return
        val next = transform(current)
        val errors = next.validateStructure()
        _uiState.update { it.copy(editorDraft = next, fieldErrors = errors, validationErrors = errors.warnings) }
        // Gate the write on validity: an invalid draft stays in memory, disk keeps the last good copy.
        if (errors.isValid) scheduleSave(next) else saveJob?.cancel()
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
        if (!item.validateStructure().isValid) {
            _uiState.update { it.copy(message = "Fix the deep link before sharing it.") }
            return
        }
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
        if (draft != null && saveJob?.isActive == true && draft.validateStructure().isValid) {
            saveJob?.cancel()
            runBlocking { configStore.saveLocal(ConfigKind.DeepLinks, draft) }
        }
        scope.cancel()
    }

    private companion object {
        val SAVE_DEBOUNCE = 500.milliseconds
    }
}
