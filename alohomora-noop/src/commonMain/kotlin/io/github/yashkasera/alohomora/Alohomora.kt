package io.github.yashkasera.alohomora

import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.replay.TraceReplayHandler
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

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
 *
 * **Every member here must mirror `:alohomora`'s `Alohomora` object exactly** — same names,
 * same parameter lists and order, same defaults, same JVM annotations. A consumer compiles
 * one call site against both artifacts, so any divergence is a release-only build failure.
 * `:consumerParity` in the root build enforces this; do not hand-edit one side alone.
 */
object Alohomora {

    fun init() {
        /* no-op */
    }

    // ============================================================================
    // Logging and Event Tracking - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    fun recordTrace(
        id: String = "",
        status: Int? = null,
        url: String? = null,
        message: String? = null,
        method: String? = null,
        scheme: String? = null,
        host: String? = null,
        path: String? = null,
        query: String? = null,
        requestBody: String? = null,
        responseBody: String? = null,
        time: Long? = null,
        duration: Long? = null,
        requestHeaders: Map<String, List<String>>? = null,
        requestContentType: String? = null,
        responseContentType: String? = null,
        responseHeaders: Map<String, List<String>>? = null,
        requestSize: Long? = null,
        responseSize: Long? = null,
    ) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    @JvmStatic
    @JvmOverloads
    fun recordTelemetry(name: String, properties: Map<String, String>? = null) {
        /* no-op */
    }

    // ============================================================================
    // DevTools TCP Server - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun startDevToolsServer(port: Int = 53999): Boolean {
        return false
    }

    fun stopDevToolsServer() {
        /* no-op */
    }

    // ============================================================================
    // App Database Overrides - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerAppDatabase(name: String, path: String? = null) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    fun excludeAppDatabase(name: String) {
        /* no-op */
    }

    fun clearAppDatabaseOverrides() {
        /* no-op */
    }

    // ============================================================================
    // Trace Replay - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerReplayHandler(handler: TraceReplayHandler) {
        /* no-op */
    }

    fun clearReplayHandler() {
        /* no-op */
    }

    val isReplaySupported: Boolean get() = false

    // ============================================================================
    // Plugin System - No-op
    // ============================================================================

    @Suppress("UNUSED_PARAMETER")
    fun registerPlugin(plugin: CustomScreenPlugin) {
        /* no-op */
    }

    @Suppress("UNUSED_PARAMETER")
    fun unregisterPlugin(pluginId: String): Boolean {
        return false
    }

    fun getPlugins(): List<CustomScreenPlugin> {
        return emptyList()
    }
}
