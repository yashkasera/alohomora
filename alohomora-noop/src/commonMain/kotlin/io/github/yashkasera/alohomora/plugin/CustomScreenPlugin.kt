package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * No-op implementation of CustomScreenPlugin for release builds.
 *
 * This interface matches the debug version but will never be used at runtime.
 *
 * Every member here must mirror `:alohomora`'s `CustomScreenPlugin` exactly — a consumer
 * implements this interface once and compiles it against both artifacts.
 */
interface CustomScreenPlugin {
    val id: String
    val title: String
    val description: String? get() = null
    val icon: ImageVector? get() = null
    val showInDashboard: Boolean get() = true
    val showInNavigation: Boolean get() = false
    val priority: Int get() = 100

    @Composable
    fun Content(onBackClick: () -> Unit)

    fun onScreenVisible() {}
    fun onScreenHidden() {}
}

