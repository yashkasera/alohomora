package io.github.yashkasera.alohomora.devtools

import io.github.yashkasera.alohomora.common.ThrottleProfile
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.vpn.VpnThrottleController
import kotlinx.coroutines.flow.StateFlow

internal actual val isVpnThrottleSupported: Boolean = true

internal actual fun vpnThrottleEnable(profile: ThrottleProfile) =
    VpnThrottleController.enable(profile)

internal actual fun vpnThrottleDisable() =
    VpnThrottleController.disable()

internal actual fun vpnThrottleStateFlow(): StateFlow<VpnThrottleState> =
    VpnThrottleController.state

internal actual fun vpnThrottleActiveProfile(): ThrottleProfile? =
    VpnThrottleController.activeProfile.value
