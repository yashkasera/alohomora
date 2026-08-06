package io.github.yashkasera.alohomora.devtools

/**
 * Shows a persistent notification while the DevTools TCP server is running.
 *
 * The notification tells the developer that Alohomora is listening, whether a desktop client is
 * connected, and tapping it opens the DevTools console. It is dismissed when the server stops.
 *
 * Platform differences:
 *  - **Android:** An ongoing, low-importance notification (no sound/vibration). Survives app
 *    backgrounding so the developer can see the server is still alive from the notification shade.
 *  - **iOS:** A local notification posted via `UNUserNotificationCenter`. Tapping it brings the
 *    app to the foreground. iOS has no concept of an "ongoing" notification, so it posts once on
 *    start and is removed on stop.
 */
internal expect object ServerActiveNotificationHost {

    /** Shows or updates the notification. [hasClient] controls the subtitle copy. */
    fun show(port: Int, hasClient: Boolean)

    /** Removes the notification. Safe to call when nothing is showing. */
    fun dismiss()
}
