package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import io.github.yashkasera.alohomora.presentation.ui.components.CanvasBackground
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.ConnectionBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.PanelCard
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.StatusPill
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SwitchingOverlay
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.ApiLogsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DatabasePanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.DevicesPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.EventsPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.LogcatPanel
import io.github.yashkasera.alohomora.desktop.presentation.ui.panels.PreferencesPanel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevicesViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.LogcatViewModel
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.PrefsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraLargeTopAppBar
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevToolsDesktopApp(
    devToolsViewModel: DevToolsViewModel,
    devicesViewModel: DevicesViewModel,
    logcatViewModel: LogcatViewModel,
    databaseViewModel: DatabaseViewModel,
    prefsViewModel: PrefsViewModel,
) {
    var activeSection by remember { mutableStateOf(DesktopSection.Devices) }

    AlohomoraTheme(onThemeChanged = {}) {
        Surface(modifier = Modifier.fillMaxSize()) {

            Scaffold(
                topBar = {
                    AlohomoraLargeTopAppBar(
                        title = {
                            Column {
                                Text(
                                    text = "Alohomora",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 34.sp,
                                )
                                Text(
                                    text = "DESKTOP DEVTOOLS CLIENT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        },
                        actions = {
                            val devToolsState by devToolsViewModel.uiState.collectAsState()
                            StatusPill(devToolsState.connection)
                            Spacer(modifier = Modifier.width(12.dp))
                            val deviceId = devToolsState.currentDeviceId
                            if (!deviceId.isNullOrBlank()) {
                                Text(
                                    text = "Device: $deviceId",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            if (activeSection == DesktopSection.Devices) {
                                AlohomoraFilledButton(
                                    text = "Refresh Devices",
                                    onClick = { devicesViewModel.refreshDevices() },
                                )
                            }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CanvasBackground()
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(100.dp)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            DesktopSection.values().forEach { section ->
                                NavigationRailItem(
                                    selected = activeSection == section,
                                    onClick = { activeSection = section },
                                    icon = {
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, MaterialTheme.colorScheme.onSurface)
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = section.railLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            ConnectionBar(viewModel = devToolsViewModel)
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(modifier = Modifier.fillMaxSize()) {
                                PanelCard(title = activeSection.title) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = activeSection.subtitle.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        when (activeSection) {
                                            DesktopSection.Devices -> DevicesPanel(
                                                devicesViewModel = devicesViewModel,
                                                devToolsViewModel = devToolsViewModel,
                                            )
                                            DesktopSection.Logcat -> LogcatPanel(
                                                devicesViewModel = devicesViewModel,
                                                logcatViewModel = logcatViewModel,
                                            )
                                            DesktopSection.Events -> EventsPanel(devToolsViewModel = devToolsViewModel)
                                            DesktopSection.ApiLogs -> ApiLogsPanel(devToolsViewModel = devToolsViewModel)
                                            DesktopSection.Database -> DatabasePanel(
                                                databaseViewModel = databaseViewModel,
                                            )
                                            DesktopSection.Preferences -> PreferencesPanel(
                                                prefsViewModel = prefsViewModel,
                                            )
                                        }
                                    }
                                }
                                val switching by devToolsViewModel.uiState.collectAsState()
                                if (switching.switching) {
                                    SwitchingOverlay()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
