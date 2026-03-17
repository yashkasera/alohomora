package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.presentation.ui.DevToolsDesktopApp
import io.github.yashkasera.alohomora.desktop.presentation.ui.DeviceSelectionScreen
import io.github.yashkasera.alohomora.desktop.util.DevicePortRegistry
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import java.awt.Dimension

fun main() = application {
    val state = rememberWindowState(placement = WindowPlacement.Maximized)
    Window(
        title = "Alohomora",
        state = state,
        onCloseRequest = ::exitApplication,
    ) {
        AppTheme {
            val theme = LocalThemeIsDark.current
            MenuBar {
                Menu("File") {
                    Item("Toggle Theme", onClick = { theme.value = !theme.value })
                    Item("Exit", onClick = ::exitApplication)
                }
            }
            window.minimumSize = Dimension(350, 600)
            val composition = remember { DesktopAppComposition() }
            val portRegistry = remember { DevicePortRegistry() }
            var host by remember { mutableStateOf("127.0.0.1") }
            var port by remember { mutableStateOf("53999") }
            val devToolsState by composition.devToolsViewModel.uiState.collectAsState()

            if (devToolsState.connection is DevToolsConnection.Connected) {
                DevToolsDesktopApp(
                    devToolsViewModel = composition.devToolsViewModel,
                    devicesViewModel = composition.devicesViewModel,
                    logcatViewModel = composition.logcatViewModel,
                    databaseViewModel = composition.databaseViewModel,
                    prefsViewModel = composition.prefsViewModel,
                    host = host,
                    port = port,
                )
            } else {
                DeviceSelectionScreen(
                    devicesViewModel = composition.devicesViewModel,
                    devToolsViewModel = composition.devToolsViewModel,
                    portRegistry = portRegistry,
                    host = host,
                    port = port,
                    onHostChange = { host = it },
                    onPortChange = { port = it.filter(Char::isDigit) },
                )
            }
        }
    }
}
