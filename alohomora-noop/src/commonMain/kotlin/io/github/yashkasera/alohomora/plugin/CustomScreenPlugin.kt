package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server

/**
 * No-op implementation of CustomScreenPlugin for release builds.
 *
 * This interface matches the debug version but will never be used at runtime.
 */
interface CustomScreenPlugin {
    val id: String
    val title: String
    val description: String? get() = null
    val icon: ImageVector get() = Icons.Server
    val showInDashboard: Boolean get() = true
    val showInNavigation: Boolean get() = false
    val priority: Int get() = 100

    @Composable
    fun Content(onBackClick: () -> Unit)

    fun onScreenVisible() {}
    fun onScreenHidden() {}
}

