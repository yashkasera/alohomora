package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.common.ActionParameter
import io.github.yashkasera.alohomora.devtools.DevToolsActionHandler

data class PluginAction(
    val id: String,
    val label: String,
    val description: String? = null,
    val parameters: List<ActionParameter> = emptyList(),
    val handler: DevToolsActionHandler,
)

/** No-op mirror of `:alohomora`'s `CustomScreenPlugin`. Must match the debug version exactly. */
@Suppress("unused")
interface CustomScreenPlugin {
    val id: String
    val title: String
    val description: String? get() = null
    val icon: ImageVector? get() = null
    val showInDashboard: Boolean get() = true
    val showInNavigation: Boolean get() = false
    val priority: Int get() = 100
    val actions: List<PluginAction> get() = emptyList()
    val dataFields: List<PluginDataField> get() = emptyList()

    @Composable
    fun Content()

    fun onScreenVisible() {}
    fun onScreenHidden() {}
}

