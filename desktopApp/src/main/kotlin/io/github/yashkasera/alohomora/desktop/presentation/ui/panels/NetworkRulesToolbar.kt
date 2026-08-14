package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.common.ThrottleProfiles
import io.github.yashkasera.alohomora.common.VpnThrottleState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.NetworkRulesViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun NetworkRulesActions(
    viewModel: NetworkRulesViewModel,
    onOpenMockRules: () -> Unit,
) {
    val supported by viewModel.networkRulesSupported.collectAsState()
    if (!supported) return

    val throttle by viewModel.throttleProfile.collectAsState()
    val mockRules by viewModel.mockRules.collectAsState()
    val currentSession by viewModel.currentSession.collectAsState()
    val vpnSupported by viewModel.vpnThrottleSupported.collectAsState()
    val vpnEnabled by viewModel.vpnEnabled.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    var showThrottleMenu by remember { mutableStateOf(false) }

    val activeThrottleLabel = when (throttle.name) {
        "none" -> null
        "edge" -> "EDGE"
        "3g" -> "3G"
        "fast_3g" -> "Fast 3G"
        "slow_wifi" -> "Slow WiFi"
        else -> throttle.name
    }

    val enabledMockCount = mockRules.count { it.enabled }

    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThrottleDropdown(
            label = activeThrottleLabel?.let { "Throttle: $it" } ?: "No throttle",
            expanded = showThrottleMenu,
            onExpandChange = { showThrottleMenu = it },
            onSelect = { viewModel.selectProfile(it) },
            isActive = activeThrottleLabel != null,
        )

        MockRulesChip(
            count = enabledMockCount,
            total = mockRules.size,
            sessionName = currentSession?.name,
            onClick = onOpenMockRules,
        )

        if (vpnSupported && activeThrottleLabel != null) {
            DeviceWideChip(
                enabled = vpnEnabled,
                state = vpnState,
                onToggle = { viewModel.toggleDeviceWideThrottle(!vpnEnabled) },
            )
        }
    }
}

@Composable
private fun ThrottleDropdown(
    label: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onSelect: (io.github.yashkasera.alohomora.common.ThrottleProfile) -> Unit,
    isActive: Boolean,
) {
    AlohomoraFilterChip(
        label = label,
        selected = isActive,
        uppercase = false,
        onClick = { onExpandChange(true) },
    )
    AlohomoraDropdownMenu(expanded = expanded, onDismissRequest = { onExpandChange(false) }) {
        ThrottleProfiles.PRESETS.forEach { profile ->
            val displayName = when (profile.name) {
                "none" -> "No throttle"
                "edge" -> "EDGE (800ms / 6 KB/s)"
                "3g" -> "3G (400ms / 40 KB/s)"
                "fast_3g" -> "Fast 3G (150ms / 200 KB/s)"
                "slow_wifi" -> "Slow WiFi (50ms / 500 KB/s)"
                else -> profile.name
            }
            AlohomoraDropdownMenuItem(
                text = { Text(displayName) },
                onClick = {
                    onExpandChange(false)
                    onSelect(profile)
                },
            )
        }
    }
}

@Composable
private fun MockRulesChip(
    count: Int,
    total: Int,
    sessionName: String?,
    onClick: () -> Unit,
) {
    val label = buildString {
        append(sessionName ?: "Mock rules")
        when {
            total == 0 -> {}
            count == 0 -> append(" ($total paused)")
            else -> append(" ($count active)")
        }
    }
    AlohomoraFilterChip(
        leadingIcon = {
            Icon(
                Icons.Server,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
            )
        },
        label = label,
        selected = count > 0,
        uppercase = false,
        onClick = onClick,
    )
}

@Composable
private fun DeviceWideChip(
    enabled: Boolean,
    state: VpnThrottleState,
    onToggle: () -> Unit,
) {
    val label = when (state) {
        VpnThrottleState.OFF -> "Device-wide"
        VpnThrottleState.AWAITING_CONSENT -> "VPN consent..."
        VpnThrottleState.STARTING -> "VPN starting..."
        VpnThrottleState.ACTIVE -> "Device-wide (VPN)"
        VpnThrottleState.ERROR -> "VPN error"
    }
    AlohomoraFilterChip(
        label = label,
        selected = enabled && state == VpnThrottleState.ACTIVE,
        uppercase = false,
        onClick = onToggle,
    )
}
