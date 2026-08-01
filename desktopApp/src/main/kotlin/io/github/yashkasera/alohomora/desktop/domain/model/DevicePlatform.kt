package io.github.yashkasera.alohomora.desktop.domain.model

/**
 * Which platform a connected device runs, and therefore how the desktop app reaches it.
 *
 * The transport differs per platform but the DevTools protocol above it does not:
 *  - [ANDROID]        — `adb forward tcp:<host> tcp:<device>`
 *  - [IOS]            — a usbmuxd tunnel over USB (`/var/run/usbmuxd`, the same mechanism
 *                       Xcode and Safari Web Inspector use). There is no adb on iOS.
 *  - [IOS_SIMULATOR]  — no tunnel at all. The simulator runs on the host's network stack, so
 *                       a server bound to 127.0.0.1 inside the simulator *is* the host's
 *                       127.0.0.1. Verified empirically, not assumed.
 */
enum class DevicePlatform {
    ANDROID,
    IOS,
    IOS_SIMULATOR,
    ;

    val isIos: Boolean get() = this == IOS || this == IOS_SIMULATOR

    /** Human label for the device list. */
    val label: String
        get() = when (this) {
            ANDROID -> "Android"
            IOS -> "iOS"
            IOS_SIMULATOR -> "iOS Simulator"
        }
}

/**
 * A discrete capability the desktop app may or may not be able to use against a device.
 *
 * Modelled explicitly so the UI can hide what a platform cannot serve. Roughly half the ADB
 * Tools panel has no iOS analogue — `devicectl` has no console subcommand, there is no
 * `dumpsys`, and `screenrecord` is Android-only. Rendering those controls for an iOS device
 * would ship buttons guaranteed to fail, which is the same defect already present elsewhere
 * in this codebase (export FAB, live-session filter, Copy JSON all render and do nothing).
 */
enum class DeviceCapability {
    /** Stream the OS log (Android logcat). No supported equivalent on iOS today. */
    OS_LOG_STREAM,

    /** Install / uninstall an app package from a local file. */
    APP_INSTALL,

    /** Poll device metrics: battery, memory, CPU, jank (all `dumpsys`-derived). */
    DEVICE_METRICS,

    /** Toggle Wi-Fi / mobile data via shell. */
    CONNECTIVITY_TOGGLES,

    /** Capture a screenshot. */
    SCREENSHOT,

    /** Record the screen. */
    SCREEN_RECORD,

    /** Collect a full bug report. */
    BUGREPORT,

    /** Run an arbitrary shell command against the device. */
    SHELL,

    /** Open a deep link. */
    DEEP_LINK,
    ;

    companion object {
        /** Android over adb can do everything the desktop app currently offers. */
        val ANDROID_CAPABILITIES: Set<DeviceCapability> = entries.toSet()

        /**
         * iOS today supports only the DevTools protocol itself (traffic, events, cache,
         * database, git history) plus app install via `devicectl`. Everything else in the ADB
         * panel is Android-shaped.
         */
        val IOS_CAPABILITIES: Set<DeviceCapability> = setOf(APP_INSTALL)

        /** A booted simulator cannot be driven like hardware; `simctl` covers install only. */
        val IOS_SIMULATOR_CAPABILITIES: Set<DeviceCapability> = setOf(APP_INSTALL, SCREENSHOT)

        fun forPlatform(platform: DevicePlatform): Set<DeviceCapability> = when (platform) {
            DevicePlatform.ANDROID -> ANDROID_CAPABILITIES
            DevicePlatform.IOS -> IOS_CAPABILITIES
            DevicePlatform.IOS_SIMULATOR -> IOS_SIMULATOR_CAPABILITIES
        }
    }
}
