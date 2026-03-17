package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.util.DevicePortRegistry
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeviceSelectionScreen(
    devicesViewModel: DevicesViewModel,
    devToolsViewModel: DevToolsViewModel,
    portRegistry: DevicePortRegistry,
    host: String,
    port: String,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
) {
    val devices by devicesViewModel.devices.collectAsState()
    val activeDeviceId by devicesViewModel.selectedDeviceId.collectAsState()
    val error by devicesViewModel.error.collectAsState()
    val devToolsState by devToolsViewModel.uiState.collectAsState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var actionError by remember { mutableStateOf<String?>(null) }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            devicesViewModel.refreshDevices()
            delay(3000)
        }

    }

    val selectedDevice = devices.firstOrNull { it.id == selectedDeviceId }
    val numericPort = port.toIntOrNull() ?: 53999
    var devicePort by remember { mutableStateOf("53999") }
    val deviceServerPort = devicePort.toIntOrNull() ?: 53999
    val canConnect = selectedDevice?.state == DeviceState.DEVICE
    val isConnecting = devToolsState.connection is DevToolsConnection.Connecting
    val isLocalHost = host == "127.0.0.1" || host == "localhost"
    val hasActiveDevice = activeDeviceId != null

    LaunchedEffect(selectedDeviceId, isLocalHost) {
        val selectedId = selectedDeviceId ?: return@LaunchedEffect
        if (isLocalHost) {
            val assigned = portRegistry.getPort(selectedId) ?: portRegistry.assignPort(selectedId)
            devicePort = assigned.toString()
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(Color(0xFFF8F9FD))
                .padding(horizontal = 16.dp, vertical = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(Color(0xFF1A56DB), CircleShape),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Alohomora.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "CONNECT",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 12.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Select a device and connect to DevTools.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Device Selection",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Choose a device, then connect.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Devices", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { devicesViewModel.refreshDevices() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Icon(Icons.RefreshCw, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Refresh")
                            }

                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (devices.isEmpty()) {
                            Text("No devices detected.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            devices.forEach { device ->
                                val assignedPort = portRegistry.getPort(device.id)
                                DeviceRow(
                                    device = device,
                                    selected = device.id == selectedDeviceId,
                                    isActive = device.id == activeDeviceId,
                                    assignedPort = assignedPort,
                                    onSelect = { selectedDeviceId = device.id },
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connection", style = MaterialTheme.typography.titleSmall)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = host,
                            onValueChange = onHostChange,
                            label = { Text("Host") },
                            singleLine = true,
                            modifier = Modifier.width(180.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = port,
                            onValueChange = onPortChange,
                            label = { Text("Host Port") },
                            singleLine = true,
                            modifier = Modifier.width(120.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedTextField(
                            value = devicePort,
                            onValueChange = {
                                val sanitized = it.filter(Char::isDigit)
                                devicePort = sanitized
                                val selectedId = selectedDeviceId
                                if (!selectedId.isNullOrBlank()) {
                                    val newPort = sanitized.toIntOrNull() ?: return@OutlinedTextField
                                    portRegistry.setPort(selectedId, newPort)
                                    onPortChange(newPort.toString())
                                }
                            },
                            label = { Text("Device Port") },
                            singleLine = true,
                            modifier = Modifier.width(120.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (selectedDevice == null) {
                                    actionError = "Select a device first"
                                    return@Button
                                }
                                val hostPort = if (isLocalHost) {
                                    portRegistry.setPort(selectedDevice.id, deviceServerPort)
                                    if (deviceServerPort.toString() != port) {
                                        onPortChange(deviceServerPort.toString())
                                    }
                                    deviceServerPort
                                } else {
                                    numericPort
                                }
                                scope.launch {
                                    if (isLocalHost) {
                                        devicesViewModel.selectDevice(
                                            selectedDevice.id,
                                            hostPort,
                                            deviceServerPort,
                                        ) { selectError ->
                                            if (selectError == null) {
                                                devToolsViewModel.switchDevice(host, hostPort, selectedDevice.id)
                                                actionError = null
                                            } else {
                                                actionError = selectError
                                            }
                                        }
                                    } else {
                                        devicesViewModel.connectOverTcp(
                                            selectedDevice.id,
                                            host,
                                            deviceServerPort,
                                        ) { connectError ->
                                            if (connectError == null) {
                                                devicesViewModel.selectDevice(
                                                    selectedDevice.id,
                                                    hostPort,
                                                    deviceServerPort,
                                                ) { selectError ->
                                                    if (selectError == null) {
                                                        devToolsViewModel.switchDevice(host, hostPort, selectedDevice.id)
                                                        actionError = null
                                                    } else {
                                                        actionError = selectError
                                                    }
                                                }
                                            } else {
                                                actionError = connectError
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = canConnect && !isConnecting,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A56DB)),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            Text(if (isConnecting) "Connecting..." else "Connect")
                        }
                    }
                    val errorMessage = actionError ?: error ?: when (val connection = devToolsState.connection) {
                        is DevToolsConnection.Failed -> connection.reason
                        else -> null
                    }
                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC62828),
                        )
                    }
                    if (hasActiveDevice) {
                        Text(
                            text = "Active device: $activeDeviceId",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceUi,
    selected: Boolean,
    isActive: Boolean,
    assignedPort: Int?,
    onSelect: () -> Unit,
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
    val background = if (selected) Color(0xFFEDF2FF) else Color(0xFFF7F7F7)
    val enabled = device.state == DeviceState.DEVICE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = device.state.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "forwarded ${assignedPort ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        when {
            isActive -> Text("Active", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A56DB))
            selected -> Text("Selected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1A56DB))
        }
    }
}
