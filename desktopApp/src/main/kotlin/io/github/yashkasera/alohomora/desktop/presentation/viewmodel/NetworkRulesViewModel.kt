package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.MockRule
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class NetworkRulesViewModel(
    private val repository: DevToolsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    fun selectProfile(profile: ThrottleProfile) {
        _throttleProfile.value = profile
        repository.setThrottleProfile(profile)
        if (vpnEnabled.value) {
            repository.setVpnThrottle(profile, enabled = true)
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

    fun clearAll() {
        _throttleProfile.value = ThrottleProfiles.NONE
        _mockRules.value = emptyList()
        repository.setThrottleProfile(ThrottleProfiles.NONE)
        repository.setMockRules(emptyList())
    }

    private fun sendRules() {
        repository.setMockRules(_mockRules.value)
    }

    fun close() {
        scope.cancel()
    }
}
