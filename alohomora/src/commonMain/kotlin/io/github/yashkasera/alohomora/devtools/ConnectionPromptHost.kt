package io.github.yashkasera.alohomora.devtools

/**
 * Surfaces a pending connection request to the user, wherever they happen to be in the app.
 *
 * The point is that the developer should not have to navigate into the Alohomora console to read
 * a code — the request comes in while they are looking at the desktop, so it has to come to them.
 *
 * Hosting differs sharply by platform, which is why this is an expect:
 *  - **Android** can overlay a sheet on the foreground Activity, but when the app is backgrounded
 *    it cannot: Android 10+ blocks background Activity starts, and a backgrounded app is the
 *    common case while debugging. It falls back to a notification.
 *  - **iOS** only ever needs the foreground path. A suspended iOS app cannot service the socket
 *    at all — the kernel completes the TCP handshake from the listen backlog while the app is
 *    frozen — so a connection that reaches auth implies a foreground app.
 */
internal expect object ConnectionPromptHost {

    /**
     * Shows the prompt for [otp].
     *
     * @param onRememberChange invoked when the user toggles "remember this computer". The
     *   Android notification fallback cannot offer the choice, so a pairing completed while
     *   backgrounded never calls this and is correctly treated as no consent.
     */
    fun show(otp: String, onRememberChange: (Boolean) -> Unit)

    /** Removes the prompt. Safe to call when nothing is showing. */
    fun dismiss()
}
