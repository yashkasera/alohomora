package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.DeviceSelectorRow
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatControls
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatFilters
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatList
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import kotlinx.coroutines.launch

@Composable
fun LogcatPanel(
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
) {
    val devices by devicesViewModel.devices.collectAsState()
    val selectedDeviceId by devicesViewModel.selectedDeviceId.collectAsState()
    val error by devicesViewModel.error.collectAsState()
    val uiState by logcatViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedDeviceId) {
        logcatViewModel.setSelectedDevice(selectedDeviceId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "Device", style = MaterialTheme.typography.labelMedium)
        Spacer(modifier = Modifier.height(8.dp))
        DeviceSelectorRow(
            devices = devices,
            selectedDeviceId = selectedDeviceId,
            onRefresh = { devicesViewModel.refreshDevices() },
            onSelect = { device ->
                scope.launch {
                    devicesViewModel.selectDevice(device.id, 53999, 53999)
                }
            }
        )

        if (!error.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error ?: "",
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!uiState.errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage ?: "",
                color = Color(0xFFC62828),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        LogcatControls(
            running = uiState.running,
            onStart = { logcatViewModel.start() },
            onStop = { logcatViewModel.stop() },
            onClear = { logcatViewModel.clear() },
        )

        Spacer(modifier = Modifier.height(12.dp))
        LogcatFilters(
            filterState = uiState.filterState,
            availableTags = uiState.availableTags,
            onToggleLevel = { logcatViewModel.toggleLevel(it) },
            onSelectTag = { logcatViewModel.updateSelectedTag(it) },
            onSearch = { logcatViewModel.updateSearchQuery(it) },
        )

        Spacer(modifier = Modifier.height(12.dp))
        LogcatList(entries = uiState.filteredEntries)
    }
}
