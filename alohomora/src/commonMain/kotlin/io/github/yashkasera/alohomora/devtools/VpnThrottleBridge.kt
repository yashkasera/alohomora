package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.VpnThrottleState
import kotlinx.coroutines.flow.StateFlow

internal expect val isVpnThrottleSupported: Boolean

internal expect fun vpnThrottleEnable(profile: ThrottleProfile)

internal expect fun vpnThrottleDisable()

internal expect fun vpnThrottleStateFlow(): StateFlow<VpnThrottleState>

internal expect fun vpnThrottleActiveProfile(): ThrottleProfile?
