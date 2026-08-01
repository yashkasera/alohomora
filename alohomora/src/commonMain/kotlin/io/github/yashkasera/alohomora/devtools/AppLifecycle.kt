package io.github.yashkasera.alohomora.devtools

/**
 * Tells the DevTools server when the host app returns to the foreground.
 *
 * Exists because iOS suspends backgrounded apps: the process stays alive and the listening socket
 * stays bound, but nothing runs to accept on it. The desktop's TCP connect completes from the
 * kernel's listen backlog and then hangs forever with no error on either side — which is exactly
 * what made this look like a transport bug rather than a lifecycle one.
 *
 * There is no way to keep the socket serviced while suspended, so the server rebinds on resume
 * and the desktop reconnects.
 */
internal expect object AppLifecycle {

    /**
     * Registers [onForeground], replacing any previous registration.
     *
     * Called on every foreground transition, not just the first.
     */
    fun observeForeground(onForeground: () -> Unit)

    /** Removes the registration. Safe to call when nothing is registered. */
    fun stopObserving()
}
