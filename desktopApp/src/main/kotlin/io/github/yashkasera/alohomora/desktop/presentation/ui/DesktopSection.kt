package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.presentation.ui.components.icons.AlertTriangle
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons

enum class DesktopSection(
    val title: String,
    val icon: ImageVector,
) {
    Dashboard("Dashboard", Icons.AlertTriangle),
    Builds("Builds", Icons.AlertTriangle),
    Devices("Devices", Icons.AlertTriangle),
    Network("Network", Icons.AlertTriangle),
    Logs("Logs", Icons.AlertTriangle),
}
