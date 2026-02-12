package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.data.model.AlohomoraBuildInfo
import io.github.yashkasera.alohomora.data.model.Commit
import io.github.yashkasera.alohomora.di.initKoin
import io.github.yashkasera.alohomora.domain.repository.EventRepository
import io.github.yashkasera.alohomora.domain.repository.LogRepository
import io.github.yashkasera.alohomora.devtools.DevToolsDefaults
import io.github.yashkasera.alohomora.devtools.DevToolsDatabaseOverrides
import io.github.yashkasera.alohomora.devtools.DevToolsRuntime
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import org.koin.core.Koin
import org.koin.dsl.KoinAppDeclaration

/**
 * Main entry point for the Alohomora debugging library.
 *
 * This object provides APIs for:
 * - Initializing the library
 * - Logging and event tracking
 * - Registering custom screens via plugins
 * - Storing and retrieving custom values
 * - Connecting to remote debugging tools
 *
 * Example usage:
 * ```kotlin
 * // Initialize the library
 * Alohomora.init()
 *
 * // Register a custom screen
 * Alohomora.registerPlugin(MyCustomScreen())
 *
 * // Store custom values
 * Alohomora.putValue("api_key", "sk_test_123")
 *
 * // Track events
 * Alohomora.trackEvent("user_login", mapOf("user_id" to "123"))
 * ```
 */
object Alohomora {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var koin: Koin? = null

    /**
     * Latest commits baked into this build.
     *
     * - Debug / staging (if enabled via plugin): populated
     * - Release / unsupported platforms: empty
     */
    var commits: List<Commit> = emptyList()

    var buildInfo: AlohomoraBuildInfo? = null

    // Initialize the library.
    // On Android, pass { androidContext(context) } in appDeclaration
    fun init(appDeclaration: KoinAppDeclaration = {}) {
        if (koin != null) return
        koin = initKoin(appDeclaration).koin
    }

    // ============================================================================
    // Logging and Event Tracking
    // ============================================================================

    fun log(
        message: String,
        tag: String = "Alohomora",
        throwable: Throwable? = null,
    ) {
        val repo = koin?.get<LogRepository>() ?: return
        scope.launch {
//            repo.addLog(level, tag, message, throwable)
        }
    }

    fun trackEvent(name: String, properties: Map<String, String>? = null) {
        val repo = koin?.get<EventRepository>() ?: return
        scope.launch {
            repo.trackEvent(name, properties?.let { Json.encodeToJsonElement(properties) })
        }
    }

    // ============================================================================
    // DevTools TCP Server
    // ============================================================================

    fun startDevToolsServer(port: Int = DevToolsDefaults.DEFAULT_PORT) {
        val runtime = koin?.get<DevToolsRuntime>() ?: return
        runtime.start(port)
    }

    fun stopDevToolsServer() {
        val runtime = koin?.get<DevToolsRuntime>() ?: return
        runtime.stop()
    }

    // ============================================================================
    // App Database Overrides
    // ============================================================================

    fun registerAppDatabase(name: String, path: String? = null) {
        DevToolsDatabaseOverrides.include(name, path)
    }

    fun excludeAppDatabase(name: String) {
        DevToolsDatabaseOverrides.exclude(name)
    }

    fun clearAppDatabaseOverrides() {
        DevToolsDatabaseOverrides.clear()
    }

    // ============================================================================
    // Plugin System - Custom Screens
    // ============================================================================

    /**
     * Register a custom screen plugin.
     *
     * Example:
     * ```kotlin
     * Alohomora.registerPlugin(MyFeatureFlagsScreen())
     * ```
     *
     * @param plugin The custom screen plugin to register
     * @throws IllegalArgumentException if a plugin with the same id is already registered
     */
    fun registerPlugin(plugin: CustomScreenPlugin) {
        PluginRegistry.register(plugin)
    }

    /**
     * Unregister a custom screen plugin by id.
     *
     * @param pluginId The id of the plugin to unregister
     * @return true if the plugin was removed, false if it wasn't found
     */
    fun unregisterPlugin(pluginId: String): Boolean {
        return PluginRegistry.unregister(pluginId)
    }

    /**
     * Get all registered custom screen plugins.
     *
     * @return List of all registered plugins
     */
    fun getPlugins(): List<CustomScreenPlugin> {
        return PluginRegistry.getAllPlugins()
    }
}
