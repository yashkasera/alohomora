package io.github.yashkasera.alohomora.desktop.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import javax.swing.RootPaneContainer

internal val MacTitleBarHeight = 28.dp

@Composable
internal fun applyMacTitleBar(window: RootPaneContainer) {
    if (!isMacOs) return
    LaunchedEffect(window) {
        window.rootPane.apply {
            putClientProperty("apple.awt.fullWindowContent", true)
            putClientProperty("apple.awt.transparentTitleBar", true)
            putClientProperty("apple.awt.windowTitleVisible", false)
        }
    }
}
