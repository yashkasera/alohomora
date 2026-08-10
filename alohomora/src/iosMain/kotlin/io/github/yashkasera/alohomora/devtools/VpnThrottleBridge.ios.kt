package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.VpnThrottleState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal actual val isVpnThrottleSupported: Boolean = false

internal actual fun vpnThrottleEnable(profile: ThrottleProfile) {}

internal actual fun vpnThrottleDisable() {}

private val noOpState = MutableStateFlow(VpnThrottleState.OFF)

internal actual fun vpnThrottleStateFlow(): StateFlow<VpnThrottleState> = noOpState

internal actual fun vpnThrottleActiveProfile(): ThrottleProfile? = null
