package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.desktop.data.local.MockSession
import io.github.yashkasera.alohomora.desktop.data.local.MockSessionStore
import io.github.yashkasera.alohomora.desktop.data.local.MockSessionSummary
import io.github.yashkasera.alohomora.desktop.data.local.importHar
import io.github.yashkasera.alohomora.desktop.data.local.toMockRule
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import java.io.File
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworkRulesViewModel(
    private val repository: DevToolsRepository,
    private val sessionStore: MockSessionStore = MockSessionStore(),
    /** App-scoped config store for "Share with team"; null disables the affordance. */
    private val configStore: io.github.yashkasera.alohomora.desktop.domain.config.ConfigStore? = null,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null

    private val _lastProposal =
        MutableStateFlow<io.github.yashkasera.alohomora.desktop.domain.config.Proposal?>(null)
    val lastProposal: StateFlow<io.github.yashkasera.alohomora.desktop.domain.config.Proposal?> =
        _lastProposal.asStateFlow()

    private val _shareMessage = MutableStateFlow<String?>(null)
    val shareMessage: StateFlow<String?> = _shareMessage.asStateFlow()

    val networkRulesSupported: StateFlow<Boolean> = repository.networkRulesSupported
    val vpnThrottleSupported: StateFlow<Boolean> = repository.vpnThrottleSupported
    val vpnState: StateFlow<VpnThrottleState> = repository.vpnState

    private val _throttleProfile = MutableStateFlow(ThrottleProfiles.NONE)
    val throttleProfile: StateFlow<ThrottleProfile> = _throttleProfile.asStateFlow()

    val vpnEnabled: StateFlow<Boolean> = repository.vpnState
        .map { it == VpnThrottleState.ACTIVE || it == VpnThrottleState.STARTING || it == VpnThrottleState.AWAITING_CONSENT }
        .stateIn(scope, SharingStarted.Eagerly, false)

    private val _mockRules = MutableStateFlow<List<MockRule>>(emptyList())
    val mockRules: StateFlow<List<MockRule>> = _mockRules.asStateFlow()

    private val _currentSession = MutableStateFlow<MockSession?>(null)
    val currentSession: StateFlow<MockSession?> = _currentSession.asStateFlow()

    private val _sessions = MutableStateFlow<List<MockSessionSummary>>(emptyList())
    val sessions: StateFlow<List<MockSessionSummary>> = _sessions.asStateFlow()

    /** Team mock sets read from the connected config repo's main line; empty when not connected. */
    private val _teamSessions = MutableStateFlow<List<MockSessionSummary>>(emptyList())
    val teamSessions: StateFlow<List<MockSessionSummary>> = _teamSessions.asStateFlow()

    init {
        scope.launch {
            val lastActive = sessionStore.loadLastActive()
            if (lastActive != null) {
                _currentSession.value = lastActive
                _mockRules.value = lastActive.rules
            }
            refreshSessionList()
        }
        scope.launch {
            repository.connectionState
                .filterIsInstance<DevToolsConnection.Connected>()
                .collect {
                    sendRules()
                    repository.setThrottleProfile(_throttleProfile.value)
                }
        }
    }

    fun selectProfile(profile: ThrottleProfile) {
        _throttleProfile.value = profile
        repository.setThrottleProfile(profile)
        if (vpnEnabled.value) {
            if (profile == ThrottleProfiles.NONE) {
                repository.setVpnThrottle(profile, enabled = false)
            } else {
                repository.setVpnThrottle(profile, enabled = true)
            }
        }
    }

    fun toggleDeviceWideThrottle(enabled: Boolean) {
        repository.setVpnThrottle(_throttleProfile.value, enabled = enabled)
    }

    @OptIn(ExperimentalUuidApi::class)
    fun addRule(rule: MockRule) {
        val withId = if (rule.id.isBlank()) rule.copy(id = Uuid.random().toString()) else rule
        _mockRules.update { it + withId }
        sendRules()
    }

    fun updateRule(rule: MockRule) {
        _mockRules.update { list -> list.map { if (it.id == rule.id) rule else it } }
        sendRules()
    }

    fun deleteRule(id: String) {
        _mockRules.update { list -> list.filter { it.id != id } }
        sendRules()
    }

    fun toggleRule(id: String) {
        _mockRules.update { list ->
            list.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
        }
        sendRules()
    }

    /**
     * Replaces the whole rule set at once, assigning ids to any that arrive blank.
     *
     * The set/clear MCP tools need a single replace operation rather than a sequence of add/delete,
     * each of which would send rules and reschedule the autosave. Mirrors [addRule]'s id handling.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun replaceRules(rules: List<MockRule>) {
        _mockRules.value =
            rules.map { if (it.id.isBlank()) it.copy(id = Uuid.random().toString()) else it }
        sendRules()
    }

    fun toggleAllRules() {
        val anyEnabled = _mockRules.value.any { it.enabled }
        _mockRules.update { list ->
            list.map { it.copy(enabled = !anyEnabled) }
        }
        sendRules()
    }

    fun loadSession(id: String) {
        scope.launch {
            val session = sessionStore.loadSession(id) ?: return@launch
            _currentSession.value = session
            _mockRules.value = session.rules
            sessionStore.setLastActive(id)
            sendRules()
        }
    }

    fun saveCurrentSession(name: String) {
        scope.launch {
            val existing = _currentSession.value
            val session = existing?.copy(
                name = name,
                rules = _mockRules.value,
                updatedAt = System.currentTimeMillis(),
            )
                ?: sessionStore.newSession(name, _mockRules.value)
            sessionStore.saveSession(session)
            sessionStore.setLastActive(session.id)
            _currentSession.value = session
            refreshSessionList()
        }
    }

    fun saveAsNewSession(name: String) {
        scope.launch {
            val session = sessionStore.newSession(name, _mockRules.value)
            sessionStore.saveSession(session)
            sessionStore.setLastActive(session.id)
            _currentSession.value = session
            refreshSessionList()
        }
    }

    fun deleteSession(id: String) {
        scope.launch {
            sessionStore.deleteSession(id)
            if (_currentSession.value?.id == id) {
                _currentSession.value = null
            }
            refreshSessionList()
        }
    }

    fun detachSession() {
        _currentSession.value = null
        scope.launch { sessionStore.setLastActive(null) }
    }

    /**
     * Imports mock rules from a HAR 1.2 capture — the one file ingestion config-as-code can't replace
     * (team distribution is Share; local persistence is the store). Returns an error string, or null on
     * success.
     */
    fun importHarFile(path: String): String? {
        val text = try {
            File(path).readText()
        } catch (e: Exception) {
            return "Failed to read file: ${e.message}"
        }
        return try {
            val harRules = importHar(text)
            if (harRules.isEmpty()) return "No 2xx responses with a body found in the HAR file."
            harRules.forEach { addRule(it) }
            null
        } catch (e: Exception) {
            "Unrecognised file: expected a HAR 1.2 export."
        }
    }

    fun addRuleFromTraffic(traffic: TrafficEntry) {
        addRule(traffic.toMockRule())
    }

    /**
     * Shares the current mock session with the team via the same ConfigStore path journeys and deep
     * links use. Requires a saved session and a connected repo; surfaces the proposal or a message.
     */
    fun shareCurrentSession() {
        val store = configStore ?: run {
            _shareMessage.value = "Team config is not connected."
            return
        }
        val session = _currentSession.value ?: run {
            _shareMessage.value = "Save the session first, then share it."
            return
        }
        scope.launch {
            val toShare = session.copy(rules = _mockRules.value)
            runCatching {
                store.shareWithTeam(
                    io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind.MockSets,
                    toShare,
                )
            }
                .onSuccess { proposal -> _lastProposal.value = proposal; _shareMessage.value = null }
                .onFailure { e -> _shareMessage.value = e.message ?: "Share failed" }
        }
    }

    private fun sendRules() {
        repository.setMockRules(_mockRules.value)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        val session = _currentSession.value ?: return
        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(500.milliseconds)
            val updated = session.copy(
                rules = _mockRules.value,
                updatedAt = System.currentTimeMillis(),
            )
            sessionStore.saveSession(updated)
            _currentSession.value = updated
            refreshSessionList()
        }
    }

    /** Loads a team mock set's rules into the working set as a local copy (the team artifact is read-only). */
    fun loadTeamSession(id: String) {
        val store = configStore ?: return
        scope.launch {
            val session = store.list(
                io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind.MockSets,
                io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope.TEAM,
            ).firstOrNull { it.value.id == id }?.value ?: return@launch
            _currentSession.value = null
            _mockRules.value = session.rules
            sessionStore.setLastActive(null)
            sendRules()
        }
    }

    private suspend fun refreshSessionList() {
        _sessions.value = sessionStore.listSessions()
        refreshTeamSessions()
    }

    /** Re-reads team mock sets — call when the sheet opens so a mid-session connect is reflected. */
    fun refreshTeam() {
        scope.launch { refreshTeamSessions() }
    }

    private suspend fun refreshTeamSessions() {
        val store = configStore ?: return
        _teamSessions.value = runCatching {
            store.list(
                io.github.yashkasera.alohomora.desktop.domain.config.ConfigKind.MockSets,
                io.github.yashkasera.alohomora.desktop.domain.config.ConfigScope.TEAM,
            ).map { item ->
                val session = item.value
                MockSessionSummary(session.id, session.name, session.rules.size, 0)
            }
        }.getOrDefault(emptyList())
    }

    fun close() {
        val session = _currentSession.value
        if (session != null) {
            autoSaveJob?.cancel()
            val updated = session.copy(
                rules = _mockRules.value,
                updatedAt = System.currentTimeMillis(),
            )
            kotlinx.coroutines.runBlocking {
                sessionStore.saveSession(updated)
            }
        }
        scope.cancel()
    }
}
