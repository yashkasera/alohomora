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
    private val lock = Any()

    @Volatile
    private var plugins: Map<String, CustomScreenPlugin> = emptyMap()

    fun register(plugin: CustomScreenPlugin) = synchronized(lock) {
        require(!plugins.containsKey(plugin.id)) {
            "A plugin with id '${plugin.id}' is already registered"
        }
        plugins = plugins + (plugin.id to plugin)
    }

    fun unregister(pluginId: String): Boolean = synchronized(lock) {
        val had = plugins.containsKey(pluginId)
        if (had) plugins = plugins - pluginId
        had
    }

    fun getPlugin(pluginId: String): CustomScreenPlugin? = plugins[pluginId]

    fun getAllPlugins(): List<CustomScreenPlugin> = plugins.values.toList()

    fun getDashboardPlugins(): List<CustomScreenPlugin> =
        plugins.values.filter { it.showInDashboard }.sortedBy { it.priority }

    fun getNavigationPlugins(): List<CustomScreenPlugin> =
        plugins.values.filter { it.showInNavigation }.sortedBy { it.priority }

    internal fun clear() = synchronized(lock) { plugins = emptyMap() }
}
