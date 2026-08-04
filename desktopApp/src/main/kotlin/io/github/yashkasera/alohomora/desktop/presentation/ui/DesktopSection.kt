package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceCapability
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.ui.icons.Activity
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.Gauge
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.icons.ScrollText
import io.github.yashkasera.alohomora.ui.icons.Settings
import io.github.yashkasera.alohomora.ui.icons.Terminal
import io.github.yashkasera.alohomora.ui.icons.Waypoints

enum class DesktopSection(
    val title: String,
    val icon: ImageVector,
    /**
     * Capability this section needs from the connected device, or null when it is served purely
     * by the DevTools protocol and therefore works on every platform.
     *
     * Gating is explicit because roughly half this UI is Android-shaped: Logcat, the ADB panel
     * and the Dashboard's metrics all rest on `logcat` / `dumpsys` / `adb shell`, none of which
     * exist on iOS. Rendering them for an iOS device would ship controls guaranteed to fail.
     */
    val requiredCapability: DeviceCapability? = null,
) {
    Dashboard("Dashboard", Icons.Gauge, DeviceCapability.DEVICE_METRICS),
    Logcat("Logcat", Icons.ScrollText, DeviceCapability.OS_LOG_STREAM),
    Adb("ADB", Icons.Terminal, DeviceCapability.SHELL),

    // Everything below is delivered over the DevTools protocol, so it is platform-agnostic.
    Traffic("Traffic", Icons.Route),
    // Directly under Traffic because the two are read together — a trace explains where a request's
    // time went. Platform-agnostic: spans arrive over the DevTools protocol like everything below.
    Traces("Traces", Icons.Waypoints),
    Events("Events", Icons.Activity),
    Database("Database", Icons.Database),
    Cache("Cache", Icons.Key),
    Errors("Errors", Icons.AlertTriangle),
    Config("Config", Icons.Settings),
    GitHistory("Git History", Icons.GitGraph),
    ;

    fun isSupportedBy(capabilities: Set<DeviceCapability>): Boolean =
        requiredCapability == null || requiredCapability in capabilities

    companion object {
        /** Sections to show for a device on [platform]. */
        fun forPlatform(platform: DevicePlatform): List<DesktopSection> {
            val capabilities = DeviceCapability.forPlatform(platform)
            return entries.filter { it.isSupportedBy(capabilities) }
        }

        /**
         * The section to land on for [platform].
         *
         * iOS has no Dashboard (no dumpsys), so defaulting to it would open every iOS window on
         * a permanently blank screen.
         */
        fun defaultFor(platform: DevicePlatform): DesktopSection =
            forPlatform(platform).firstOrNull() ?: Traffic
    }
}
