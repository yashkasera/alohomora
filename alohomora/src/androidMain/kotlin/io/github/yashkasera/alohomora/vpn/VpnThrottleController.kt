package io.github.yashkasera.alohomora.vpn

import android.content.Intent
import android.net.VpnService
import io.github.yashkasera.alohomora.ActivityTracker
import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.VpnThrottleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Bridge between the DevTools protocol and Android's VpnService.
 *
 * Singleton, like [NetworkRuleEngine][io.github.yashkasera.alohomora.devtools.NetworkRuleEngine]:
 * the VPN lifecycle is process-global and outlives any single DevTools connection. DevToolsRuntime
 * calls [enable]/[disable] on message receipt and observes [state] to report back to the desktop.
 */
internal object VpnThrottleController {

    private val _state = MutableStateFlow(VpnThrottleState.OFF)
    val state: StateFlow<VpnThrottleState> = _state.asStateFlow()

    private val _activeProfile = MutableStateFlow<ThrottleProfile?>(null)
    val activeProfile: StateFlow<ThrottleProfile?> = _activeProfile.asStateFlow()

    private var pendingProfile: ThrottleProfile? = null

    fun enable(profile: ThrottleProfile) {
        println("[Alohomora] VPN throttle requested: ${profile.name} (${profile.latencyMs}ms, ${profile.downloadBytesPerSec} B/s)")
        _activeProfile.value = profile

        if (_state.value == VpnThrottleState.ACTIVE) {
            updateProfile(profile)
            return
        }

        val context = ActivityTracker.applicationContext ?: run {
            _state.value = VpnThrottleState.ERROR
            println("[Alohomora] VPN: No application context available")
            return
        }

        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent == null) {
            startService(profile)
        } else {
            pendingProfile = profile
            _state.value = VpnThrottleState.AWAITING_CONSENT
            val activity = ActivityTracker.currentActivity
            if (activity != null) {
                activity.startActivity(
                    Intent(activity, VpnConsentActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } else {
                context.startActivity(
                    Intent(context, VpnConsentActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    fun onConsentResult(granted: Boolean) {
        if (granted) {
            val profile = pendingProfile ?: _activeProfile.value
            pendingProfile = null
            if (profile != null) {
                startService(profile)
            } else {
                _state.value = VpnThrottleState.OFF
            }
        } else {
            pendingProfile = null
            _state.value = VpnThrottleState.OFF
            _activeProfile.value = null
            println("[Alohomora] VPN: User denied consent")
        }
    }

    fun onServiceStateChanged(serviceState: VpnServiceState) {
        when (serviceState) {
            is VpnServiceState.Running -> {
                _state.value = VpnThrottleState.ACTIVE
                _activeProfile.value = serviceState.profile
            }
            is VpnServiceState.Stopped -> {
                _state.value = VpnThrottleState.OFF
                _activeProfile.value = null
            }
            is VpnServiceState.Error -> {
                _state.value = VpnThrottleState.ERROR
                println("[Alohomora] VPN error: ${serviceState.message}")
            }
        }
    }

    private fun updateProfile(profile: ThrottleProfile) {
        val context = ActivityTracker.applicationContext ?: return
        _activeProfile.value = profile
        val intent = Intent(context, AlohomoraVpnService::class.java).apply {
            action = AlohomoraVpnService.ACTION_UPDATE_PROFILE
            putExtra(AlohomoraVpnService.EXTRA_PROFILE, Json.encodeToString(profile))
        }
        context.startService(intent)
    }

    private fun startService(profile: ThrottleProfile) {
        val context = ActivityTracker.applicationContext ?: run {
            _state.value = VpnThrottleState.ERROR
            return
        }
        _state.value = VpnThrottleState.STARTING
        val intent = Intent(context, AlohomoraVpnService::class.java).apply {
            action = AlohomoraVpnService.ACTION_START
            putExtra(AlohomoraVpnService.EXTRA_PROFILE, Json.encodeToString(profile))
        }
        context.startForegroundService(intent)
    }

    fun disable() {
        println("[Alohomora] VPN throttle disabled")
        pendingProfile = null
        val context = ActivityTracker.applicationContext
        if (context != null && _state.value != VpnThrottleState.OFF) {
            val intent = Intent(context, AlohomoraVpnService::class.java).apply {
                action = AlohomoraVpnService.ACTION_STOP
            }
            context.startService(intent)
        }
        _activeProfile.value = null
        _state.value = VpnThrottleState.OFF
    }
}
