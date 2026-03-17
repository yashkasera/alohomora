package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.theme.logError
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

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Logcat",
                subtitle = "Live device logs with filters",
                showDivider = false,
                actions = {
                    LogcatControls(
                        running = uiState.running,
                        onStart = { logcatViewModel.start() },
                        onStop = { logcatViewModel.stop() },
                    ) {
                        logcatViewModel.clear()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
    ){
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!error.isNullOrBlank()) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.logError,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Text(
                    text = uiState.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.logError,
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
