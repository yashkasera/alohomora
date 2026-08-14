package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.desktop.data.local.MockExportEnvelope
import io.github.yashkasera.alohomora.desktop.data.local.MockSession
import io.github.yashkasera.alohomora.desktop.data.local.MockSessionStore
import io.github.yashkasera.alohomora.desktop.data.local.MockSessionSummary
import io.github.yashkasera.alohomora.desktop.data.local.exportJson
import io.github.yashkasera.alohomora.desktop.data.local.importHar
import io.github.yashkasera.alohomora.desktop.data.local.toExportEnvelope
import io.github.yashkasera.alohomora.desktop.data.local.toMockRule
import io.github.yashkasera.alohomora.desktop.data.local.toSession
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
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var autoSaveJob: Job? = null

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

    fun exportSession(path: String) {
        scope.launch {
            val session = _currentSession.value ?: MockSession(
                id = "",
                name = "Exported rules",
                rules = _mockRules.value,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
            val envelope = session.toExportEnvelope()
            val json = exportJson.encodeToString(MockExportEnvelope.serializer(), envelope)
            File(path).writeText(json)
        }
    }

    fun importFromFile(path: String): String? {
        val text = try {
            File(path).readText()
        } catch (e: Exception) {
            return "Failed to read file: ${e.message}"
        }
        val rules = try {
            val envelope = exportJson.decodeFromString(MockExportEnvelope.serializer(), text)
            val session = envelope.toSession()
            scope.launch {
                sessionStore.saveSession(session)
                sessionStore.setLastActive(session.id)
                _currentSession.value = session
                _mockRules.value = session.rules
                refreshSessionList()
                sendRules()
            }
            return null
        } catch (_: Exception) {
        }

        return try {
            val harRules = importHar(text)
            if (harRules.isEmpty()) return "No 2xx responses with body found in HAR"
            harRules.forEach { addRule(it) }
            null
        } catch (e: Exception) {
            "Unrecognised format: expected .alohomora-mocks.json or HAR 1.2"
        }
    }

    fun addRuleFromTraffic(traffic: TrafficEntry) {
        addRule(traffic.toMockRule())
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

    private suspend fun refreshSessionList() {
        _sessions.value = sessionStore.listSessions()
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
