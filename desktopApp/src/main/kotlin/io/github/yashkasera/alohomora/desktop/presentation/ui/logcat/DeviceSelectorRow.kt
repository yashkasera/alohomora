package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi

@Composable
fun DeviceSelectorRow(
    devices: List<DeviceUi>,
    selectedDeviceId: String?,
    onRefresh: () -> Unit,
    onSelect: (DeviceUi) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = devices.firstOrNull { it.id == selectedDeviceId }
    val selectorLabel = selected?.let { deviceLabel(it) } ?: "Select device"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = selectorLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Device") },
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(end = 8.dp)
                    .background(Color.Transparent)
                    .clickable { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (devices.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No devices found") },
                        onClick = { expanded = false }
                    )
                } else {
                    devices.forEach { device ->
                        val enabled = device.state == DeviceState.DEVICE
                        DropdownMenuItem(
                            text = { Text(deviceLabel(device)) },
                            onClick = {
                                expanded = false
                                if (enabled) onSelect(device)
                            },
                            enabled = enabled,
                        )
                    }
                }
            }
        }
        Button(onClick = onRefresh) {
            Text("Refresh")
        }
    }
}

private fun deviceLabel(device: DeviceUi): String {
    val suffix = when (device.state) {
        DeviceState.DEVICE -> "online"
        DeviceState.OFFLINE -> "offline"
        DeviceState.UNAUTHORIZED -> "unauthorized"
        DeviceState.UNKNOWN -> "unknown"
    }
    return buildString {
        append(device.model ?: device.id)
        if (device.model != null) {
            append(" (")
            append(device.id)
            append(")")
        }
        append(" • ")
        append(suffix)
    }
}
