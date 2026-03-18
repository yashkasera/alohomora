package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server

/**
 * Interface for custom screens that can be added to Alohomora by library users.
 *
 * Example usage:
 * ```kotlin
 * class MyFeatureFlagsScreen : CustomScreenPlugin {
 *     override val id = "feature_flags"
 *     override val title = "Feature Flags"
 *     override val icon: ImageVector? = Icons.Default.Flag
 *
 *     @Composable
 *     override fun Content(onBackClick: () -> Unit) {
 *         // Your screen implementation
 *     }
 * }
 *
 * // Register in your app
 * Alohomora.registerPlugin(MyFeatureFlagsScreen())
 * ```
 */
interface CustomScreenPlugin {
    /**
     * Unique identifier for this screen.
     * Must be unique across all registered plugins.
     */
    val id: String

    /**
     * Display title for the screen (shown in navigation and headers).
     */
    val title: String

    /**
     * Optional description for the screen.
     */
    val description: String?
        get() = null

    /**
     * Optional icon to display in navigation.
     */
    val icon: ImageVector
        get() = Icons.Server

    /**
     * Whether this screen should be shown in the dashboard as a card.
     * Default: true
     */
    val showInDashboard: Boolean
        get() = true

    /**
     * Whether this screen should be shown in the navigation rail (desktop).
     * Default: false (only dashboard screens are shown by default)
     */
    val showInNavigation: Boolean
        get() = false

    /**
     * Priority for ordering in the dashboard/navigation (lower = higher priority).
     * Default: 100
     */
    val priority: Int
        get() = 100

    /**
     * The composable content of this screen.
     *
     * @param onBackClick Callback to navigate back. Call this when user taps back button.
     */
    @Composable
    fun Content(onBackClick: () -> Unit)

    /**
     * Optional lifecycle callback when screen becomes visible.
     */
    fun onScreenVisible() {}

    /**
     * Optional lifecycle callback when screen is hidden.
     */
    fun onScreenHidden() {}
}

/**
 * Registry for managing custom screen plugins.
 */
internal object PluginRegistry {
    private val plugins = mutableMapOf<String, CustomScreenPlugin>()

    /**
     * Register a custom screen plugin.
     *
     * @param plugin The plugin to register
     * @throws IllegalArgumentException if a plugin with the same id is already registered
     */
    fun register(plugin: CustomScreenPlugin) {
        require(!plugins.containsKey(plugin.id)) {
            "A plugin with id '${plugin.id}' is already registered"
        }
        plugins[plugin.id] = plugin
    }

    /**
     * Unregister a plugin by id.
     *
     * @param pluginId The id of the plugin to unregister
     * @return true if a plugin was removed, false if no plugin with that id was found
     */
    fun unregister(pluginId: String): Boolean {
        return plugins.remove(pluginId) != null
    }

    /**
     * Get a plugin by id.
     *
     * @param pluginId The id of the plugin to retrieve
     * @return The plugin, or null if not found
     */
    fun getPlugin(pluginId: String): CustomScreenPlugin? {
        return plugins[pluginId]
    }

    /**
     * Get all registered plugins.
     *
     * @return List of all registered plugins
     */
    fun getAllPlugins(): List<CustomScreenPlugin> {
        return plugins.values.toList()
    }

    /**
     * Get plugins that should be shown in the dashboard, sorted by priority.
     */
    fun getDashboardPlugins(): List<CustomScreenPlugin> {
        return plugins.values
            .filter { it.showInDashboard }
            .sortedBy { it.priority }
    }

    /**
     * Get plugins that should be shown in navigation, sorted by priority.
     */
    fun getNavigationPlugins(): List<CustomScreenPlugin> {
        return plugins.values
            .filter { it.showInNavigation }
            .sortedBy { it.priority }
    }

    /**
     * Clear all registered plugins (useful for testing).
     */
    internal fun clear() {
        plugins.clear()
    }
}
