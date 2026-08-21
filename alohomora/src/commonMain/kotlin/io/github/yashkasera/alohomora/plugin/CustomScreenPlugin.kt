package io.github.yashkasera.alohomora.plugin

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.common.ActionParameter
import io.github.yashkasera.alohomora.devtools.DevToolsActionHandler
import kotlin.concurrent.Volatile
import kotlinx.atomicfu.locks.ReentrantLock
import kotlinx.atomicfu.locks.withLock

data class PluginAction(
    val id: String,
    val label: String,
    val description: String? = null,
    val parameters: List<ActionParameter> = emptyList(),
    val handler: DevToolsActionHandler,
)

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
 *     override fun Content() {
 *         // Your screen implementation
 *     }
 * }
 *
 * // Register in your app
 * Alohomora.registerPlugin(MyFeatureFlagsScreen())
 * ```
 */
@Suppress("unused")
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
     * Optional icon to display in navigation. When null, Alohomora supplies a default.
     *
     * Deliberately nullable so this interface depends only on `compose.ui`, never on
     * Alohomora's own icon set — the release-build mirror in `alohomora-noop` has to
     * declare the identical signature without pulling in the whole design system.
     */
    val icon: ImageVector?
        get() = null

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
     */
    @Composable
    fun Content()

    /**
     * Actions the desktop DevTools can trigger remotely for this plugin.
     *
     * Registered and unregistered automatically alongside the plugin itself.
     */
    val actions: List<PluginAction>
        get() = emptyList()

    /**
     * Data fields the desktop DevTools can view and edit.
     *
     * Registered and unregistered automatically alongside the plugin itself.
     * After changing the backing state that [PluginDataField.value] reads from,
     * call [io.github.yashkasera.alohomora.Alohomora.publishPluginData] to push
     * the new snapshot to the desktop.
     */
    val dataFields: List<PluginDataField>
        get() = emptyList()

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
 * Marks a plugin that Alohomora itself registers, as opposed to one a consumer passed to
 * [io.github.yashkasera.alohomora.Alohomora.registerPlugin].
 *
 * The overview grid uses this to keep its own modules under "SYSTEM MODULES" and put
 * consumer-registered ones under "CUSTOM MODULES". Deliberately a separate internal marker
 * rather than a property on [CustomScreenPlugin]: the distinction is meaningless to a
 * consumer, and every member of that interface has to be mirrored in `alohomora-noop`.
 */
internal interface InternalPlugin

/**
 * Registry for managing custom screen plugins.
 */
@Suppress("unused")
internal object PluginRegistry {
    private val lock = ReentrantLock()

    @Volatile
    private var plugins: Map<String, CustomScreenPlugin> = emptyMap()

    /**
     * Registers [plugin], ignoring the call if its id is already taken.
     *
     * Deliberately does not throw: this runs from `AlohomoraInitializer` during the host
     * app's startup, and a debug tool must never be able to abort app launch.
     *
     * @return true if the plugin was registered, false if the id was already in use.
     */
    fun register(plugin: CustomScreenPlugin): Boolean = lock.withLock {
        if (plugins.containsKey(plugin.id)) {
            println("[Alohomora] Plugin id '${plugin.id}' is already registered; ignoring.")
            return@withLock false
        }
        plugins = plugins + (plugin.id to plugin)
        true
    }

    fun unregister(pluginId: String): Boolean = lock.withLock {
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

    internal fun clear() = lock.withLock { plugins = emptyMap() }
}
