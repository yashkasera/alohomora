package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatControls
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatFilters
import io.github.yashkasera.alohomora.desktop.presentation.ui.logcat.LogcatList
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel

@Composable
fun LogcatPanel(
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    modifier: Modifier = Modifier,
) {
    val selectedDeviceId by devicesViewModel.selectedDeviceId.collectAsState()
    val error by devicesViewModel.error.collectAsState()
    val uiState by logcatViewModel.uiState.collectAsState()

    LaunchedEffect(selectedDeviceId) {
        logcatViewModel.setSelectedDevice(selectedDeviceId)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        "Logcat",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Live device logs with filters",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                LogcatControls(
                    running = uiState.running,
                    onStart = { logcatViewModel.start() },
                    onStop = { logcatViewModel.stop() },
                    onClear = { logcatViewModel.clear() },
                )
            }

            if (!error.isNullOrBlank()) {
                Text(
                    text = error ?: "",
                    color = Color(0xFFC62828),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = Color(0xFFC62828),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            LogcatFilters(
                filterState = uiState.filterState,
                onToggleLevel = { logcatViewModel.toggleLevel(it) },
                onSelectTag = { logcatViewModel.updateSelectedTag(it) },
                onPackageChange = { logcatViewModel.updatePackageName(it) },
                onSearch = { logcatViewModel.updateSearchQuery(it) },
            )

            LogcatList(
                entries = uiState.filteredEntries,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
