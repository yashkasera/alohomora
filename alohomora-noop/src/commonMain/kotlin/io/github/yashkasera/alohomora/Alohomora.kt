package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import kotlinx.serialization.json.JsonElement

/**
 * No-op implementation of Alohomora for release builds.
 *
 * This version has zero runtime overhead - all methods are empty and will be
 * optimized away by the compiler/ProGuard/R8.
 *
 * Use this in your release builds:
 * ```
 * dependencies {
 *     debugImplementation("io.github.yashkasera:alohomora:1.0.0")
 *     releaseImplementation("io.github.yashkasera:alohomora-noop:1.0.0")
 * }
 * ```
 */
object Alohomora {

    @Suppress("UNUSED_PARAMETER")
    fun init(appDeclaration: Any = Unit) {
        // No-op
    }

    // ============================================================================
    // Logging and Event Tracking - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun log(
        message: String,
        tag: String = "Alohomora",
        throwable: Throwable? = null,
    ) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun trackEvent(name: String, properties: Map<String, Any?>? = null) {
        // No-op
    }

    // ============================================================================
    // Remote Connection - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun connect(url: String) {
        // No-op
    }

    // ============================================================================
    // Plugin System - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerPlugin(plugin: CustomScreenPlugin) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun unregisterPlugin(pluginId: String): Boolean {
        return false
    }

    fun getPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }

    // ============================================================================
    // Custom Value Store - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: String) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: Int) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: Long) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: Boolean) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: Float) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: Double) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun putValue(key: String, value: JsonElement) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun <reified T> putObject(key: String, value: T) {
        // No-op
    }

    @Suppress("UNUSED_PARAMETER")
    fun getString(key: String, defaultValue: String? = null): String? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getInt(key: String, defaultValue: Int? = null): Int? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getLong(key: String, defaultValue: Long? = null): Long? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getFloat(key: String, defaultValue: Float? = null): Float? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getDouble(key: String, defaultValue: Double? = null): Double? {
        return defaultValue
    }

    @Suppress("UNUSED_PARAMETER")
    fun getJson(key: String): JsonElement? {
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    inline fun <reified T> getObject(key: String): T? {
        return null
    }

    @Suppress("UNUSED_PARAMETER")
    fun removeValue(key: String): Boolean {
        return false
    }

    @Suppress("UNUSED_PARAMETER")
    fun hasValue(key: String): Boolean {
        return false
    }

    fun getAllKeys(): Set<String> {
        return emptySet()
    }

    fun clearAllValues() {
        // No-op
    }
}
