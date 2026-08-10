package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.icons.Gauge
import io.github.yashkasera.alohomora.ui.icons.Globe
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun NetworkRulesToolbar(
    viewModel: NetworkRulesViewModel,
    modifier: Modifier = Modifier,
) {
    val supported by viewModel.networkRulesSupported.collectAsState()
    if (!supported) return

    val throttle by viewModel.throttleProfile.collectAsState()
    val mockRules by viewModel.mockRules.collectAsState()
    val vpnSupported by viewModel.vpnThrottleSupported.collectAsState()
    val vpnEnabled by viewModel.vpnEnabled.collectAsState()
    val vpnState by viewModel.vpnState.collectAsState()
    var showThrottleMenu by remember { mutableStateOf(false) }
    var showMockDialog by remember { mutableStateOf(false) }

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
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Gauge,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = MaterialTheme.dimens.margin.xs),
        )

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
            onClick = { showMockDialog = true },
        )

        if (vpnSupported) {
            DeviceWideChip(
                enabled = vpnEnabled,
                state = vpnState,
                onToggle = { viewModel.toggleDeviceWideThrottle(!vpnEnabled) },
            )
        }
    }

    if (showMockDialog) {
        MockRuleDialog(
            rules = mockRules,
            onAddRule = viewModel::addRule,
            onUpdateRule = viewModel::updateRule,
            onDeleteRule = viewModel::deleteRule,
            onToggleRule = viewModel::toggleRule,
            onDismiss = { showMockDialog = false },
        )
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
    DropdownMenu(expanded = expanded, onDismissRequest = { onExpandChange(false) }) {
        ThrottleProfiles.PRESETS.forEach { profile ->
            val displayName = when (profile.name) {
                "none" -> "No throttle"
                "edge" -> "EDGE (800ms / 6 KB/s)"
                "3g" -> "3G (400ms / 40 KB/s)"
                "fast_3g" -> "Fast 3G (150ms / 200 KB/s)"
                "slow_wifi" -> "Slow WiFi (50ms / 500 KB/s)"
                else -> profile.name
            }
            DropdownMenuItem(
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
    onClick: () -> Unit,
) {
    val label = when {
        total == 0 -> "Mock rules"
        count == 0 -> "Mock rules ($total paused)"
        else -> "Mock rules ($count active)"
    }
    AlohomoraFilterChip(
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
