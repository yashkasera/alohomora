package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.util.DevicePortRegistry
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.theme.brand
import io.github.yashkasera.alohomora.ui.theme.muted
import io.github.yashkasera.alohomora.ui.theme.success
import io.github.yashkasera.alohomora.ui.theme.warning
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
    PermanentNavigationDrawer(
        drawerContent = {
            PermanentDrawerSheet(
                modifier = Modifier,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.brand),
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
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select a device and connect to DevTools.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp),
                    )
                }
            }
        },
    ) {
        Scaffold(
            topBar = {
                AlohomoraTopBar(
                    title = "Device Selection",
                    subtitle = "Choose a device, then connect.",
                    showDivider = false,
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .padding(horizontal = 40.dp, vertical = 24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
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
                                    shape = MaterialTheme.shapes.small,
                                    contentPadding = PaddingValues(
                                        horizontal = 16.dp,
                                        vertical = 8.dp,
                                    ),
                                ) {
                                    Icon(
                                        Icons.RefreshCw,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Refresh")
                                }

                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (devices.isEmpty()) {
                                Text(
                                    "No devices detected.",
                                    style = MaterialTheme.typography.bodySmall,
                                )
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

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Connection", style = MaterialTheme.typography.titleSmall)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AlohomoraTextField(
                                value = host,
                                onValueChange = onHostChange,
                                label = "Host",
                                singleLine = true,
                                modifier = Modifier.width(180.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AlohomoraTextField(
                                value = port,
                                onValueChange = onPortChange,
                                label = "Host Port",
                                singleLine = true,
                                modifier = Modifier.width(120.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            AlohomoraTextField(
                                value = devicePort,
                                onValueChange = {
                                    val sanitized = it.filter(Char::isDigit)
                                    devicePort = sanitized
                                    val selectedId = selectedDeviceId
                                    if (!selectedId.isNullOrBlank()) {
                                        val newPort =
                                            sanitized.toIntOrNull() ?: return@AlohomoraTextField
                                        portRegistry.setPort(selectedId, newPort)
                                        onPortChange(newPort.toString())
                                    }
                                },
                                label = "Device Port",
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
                                                    devToolsViewModel.switchDevice(
                                                        host,
                                                        hostPort,
                                                        selectedDevice.id,
                                                    )
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
                                                            devToolsViewModel.switchDevice(
                                                                host,
                                                                hostPort,
                                                                selectedDevice.id,
                                                            )
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
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(
                                    horizontal = 20.dp,
                                    vertical = 10.dp,
                                ),
                            ) {
                                Text(if (isConnecting) "Connecting..." else "Connect")
                            }
                        }
                        val errorMessage =
                            actionError ?: error ?: when (val connection =
                                devToolsState.connection) {
                                is DevToolsConnection.Failed -> connection.reason
                                else -> null
                            }
                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (hasActiveDevice) {
                            Text(
                                text = "Active device: $activeDeviceId",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                        }
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
        DeviceState.DEVICE -> MaterialTheme.colorScheme.success
        DeviceState.OFFLINE -> MaterialTheme.colorScheme.warning
        DeviceState.UNAUTHORIZED -> MaterialTheme.colorScheme.error
        DeviceState.UNKNOWN -> MaterialTheme.colorScheme.muted
    }
    val title = buildString {
        append(device.model ?: device.id)
        if (device.model != null) {
            append(" (")
            append(device.id)
            append(")")
        }
    }
    val containerColor =
        if (selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface

    val enabled = device.state == DeviceState.DEVICE

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, MaterialTheme.shapes.small)
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled) { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = contentColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = device.state.name.lowercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "forwarded ${assignedPort ?: "-"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.6f),
                )
            }
        }
        when {
            isActive -> Text(
                "Active",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            selected -> Text(
                "Selected",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
