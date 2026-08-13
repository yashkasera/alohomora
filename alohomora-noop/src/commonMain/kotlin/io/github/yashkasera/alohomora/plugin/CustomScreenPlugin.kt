package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** No-op mirror of `:alohomora`'s `CustomScreenPlugin`. Must match the debug version exactly. */
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

