package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import androidx.compose.runtime.Composable
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin

internal object NavigationPlugin : CustomScreenPlugin {
    override val id: String
        get() = "alohomora_navigation_plugin"
    override val title: String
        get() = "Navigation Logs"

    @Composable
    override fun Content(onBackClick: () -> Unit) {
        NavigationHistoryScreen(onBackClick = onBackClick)
    }
}
