package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.yashkasera.alohomora.desktop.presentation.ui.DevToolsDesktopApp
import java.awt.Dimension

fun main() = application {
    Window(
        title = "Alohomora",
        state = rememberWindowState(width = 800.dp, height = 600.dp),
        onCloseRequest = ::exitApplication,
    ) {
        window.minimumSize = Dimension(350, 600)
        val composition = remember { DesktopAppComposition() }
        DevToolsDesktopApp(
            devToolsViewModel = composition.devToolsViewModel,
            devicesViewModel = composition.devicesViewModel,
            logcatViewModel = composition.logcatViewModel,
            databaseViewModel = composition.databaseViewModel,
            prefsViewModel = composition.prefsViewModel,
        )
    }
}
