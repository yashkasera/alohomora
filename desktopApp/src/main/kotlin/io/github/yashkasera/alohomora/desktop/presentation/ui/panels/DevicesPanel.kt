package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.util.pickApkPath
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedTextField
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DevicesPanel(
    devicesViewModel: DevicesViewModel,
    devToolsViewModel: DevToolsViewModel,
) {
    val devices by devicesViewModel.devices.collectAsState()
    val selectedId by devicesViewModel.selectedDeviceId.collectAsState()
    val lastOutput by devicesViewModel.lastCommandResult.collectAsState()
    val error by devicesViewModel.error.collectAsState()
    val connectionState by devToolsViewModel.uiState.collectAsState()
    var commandText by remember { mutableStateOf("shell getprop ro.product.model") }
    var apkPath by remember { mutableStateOf("") }
    var packageName by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000)
        }
    }

    var lastSignature by remember { mutableStateOf("") }
    LaunchedEffect(devices, selectedId, connectionState) {
        val signature = devices.joinToString("|") { "${it.id}:${it.state}" }
        if (signature != lastSignature) {
            lastSignature = signature
            if (!selectedId.isNullOrBlank() && connectionState.connection is DevToolsConnection.Connected) {
                devToolsViewModel.requestInitialState()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = "Connected Devices", style = MaterialTheme.typography.labelMedium)
            AlohomoraFilledButton(
                text = "Refresh",
                onClick = { devicesViewModel.refreshDevices() },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (devices.isEmpty()) {
                Text(text = "No devices detected.", style = MaterialTheme.typography.bodySmall)
            } else {
                devices.forEach { device ->
                    DeviceRow(
                        device = device,
                        isSelected = device.id == selectedId,
                        onSelect = {
                            scope.launch {
                                devicesViewModel.selectDevice(device.id, 53999, 53999) { errorMessage ->
                                    if (errorMessage == null) {
                                        devToolsViewModel.switchDevice("127.0.0.1", 53999, device.id)
                                    }
                                }
                            }
                        },
                        onDisconnect = {
                            scope.launch {
                                devicesViewModel.deactivateDevice(53999)
                                devToolsViewModel.disconnect()
                            }
                        }
                    )
                    AlohomoraHorizontalDivider()
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "ADB Command", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlohomoraOutlinedTextField(
                value = commandText,
                onValueChange = { commandText = it },
                placeholder = { Text("Command") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AlohomoraFilledButton(
                text = "Run",
                onClick = { devicesViewModel.runCommand(selectedId, commandText) },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        val outputText = buildString {
            if (lastOutput != null) {
                if (lastOutput!!.stdout.isNotBlank()) {
                    append(lastOutput!!.stdout)
                }
                if (lastOutput!!.stderr.isNotBlank()) {
                    if (isNotEmpty()) append("\n")
                    append(lastOutput!!.stderr)
                }
            }
        }
        if (outputText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.dp, Color(0xFFE0D7CC))
                    .padding(8.dp)
            ) {
                Text(
                    text = outputText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "Install / Uninstall", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlohomoraOutlinedTextField(
                value = apkPath,
                onValueChange = { apkPath = it },
                placeholder = { Text("APK path") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AlohomoraFilledButton(
                text = "Browse",
                onClick = {
                val picked = pickApkPath()
                if (!picked.isNullOrBlank()) {
                    apkPath = picked
                }
            })
            Spacer(modifier = Modifier.width(8.dp))
            AlohomoraFilledButton(
                text = "Install",
                onClick = { devicesViewModel.installApk(selectedId, apkPath) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlohomoraOutlinedTextField(
                value = packageName,
                onValueChange = { packageName = it },
                placeholder = { Text("Package") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            AlohomoraFilledButton(
                text = "Uninstall",
                onClick = { devicesViewModel.uninstallPackage(selectedId, packageName) },
            )
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceUi,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val statusColor = when (device.state) {
        DeviceState.DEVICE -> Color(0xFF2E7D32)
        DeviceState.OFFLINE -> Color(0xFFB27A00)
        DeviceState.UNAUTHORIZED -> Color(0xFFC62828)
        DeviceState.UNKNOWN -> Color(0xFF8A8A8A)
    }
    val title = buildString {
        append(device.model ?: device.id)
        if (device.model != null) {
            append(" (")
            append(device.id)
            append(")")
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = device.state.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        if (isSelected) {
            AlohomoraFilledButton(
                text = "Disconnect",
                onClick = onDisconnect,
            )
        } else {
            AlohomoraFilledButton(
                text = "Connect",
                onClick = onSelect,
                enabled = device.state == DeviceState.DEVICE,
            )
        }
    }
}
