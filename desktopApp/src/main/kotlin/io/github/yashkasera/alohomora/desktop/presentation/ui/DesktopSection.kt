package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Download
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Settings

enum class DesktopSection(
    val title: String,
    val icon: ImageVector,
) {
    Dashboard("Dashboard", Icons.AlertTriangle),
    Builds("Builds", Icons.Download),
    Logcat("Logcat", Icons.HardDrive),
    Adb("ADB", Icons.Server),
    Traces("Traces", Icons.Server),
    TelemetryEvents("Telemetry Events", Icons.ChartLine),
    Preferences("Preferences", Icons.AlertTriangle),
    Config("Config", Icons.Settings),
    Chronicle("Chronicle", Icons.GitGraph),
}
