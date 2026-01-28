package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable

/**
 * No-op implementation of CustomScreenPlugin for release builds.
 *
 * This interface matches the debug version but will never be used at runtime.
 */
interface CustomScreenPlugin {
    val id: String
    val title: String
    val description: String? get() = null
    val icon: Any? get() = null
    val showInDashboard: Boolean get() = true
    val showInNavigation: Boolean get() = false
    val priority: Int get() = 100

    @Composable
    fun Content(onBackClick: () -> Unit)

    fun onScreenVisible() {}
    fun onScreenHidden() {}
}

/**
 * No-op implementation of PluginRegistry for release builds.
 */
object PluginRegistry {
    @Suppress("UNUSED_PARAMETER")
    fun register(plugin: CustomScreenPlugin) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun unregister(pluginId: String): Boolean {
        return false
    }

    @Suppress("UNUSED_PARAMETER")
    fun getPlugin(pluginId: String): CustomScreenPlugin? {
        return null
    }

    fun getAllPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }

    fun getDashboardPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }

    fun getNavigationPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }

    internal fun clear() {
        // No-op
    }
}
