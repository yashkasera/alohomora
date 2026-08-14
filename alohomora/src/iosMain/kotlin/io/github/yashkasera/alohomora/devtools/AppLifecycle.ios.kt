package io.github.yashkasera.alohomora.devtools

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIBackgroundTaskInvalid
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

internal actual object AppLifecycle {

    private var foregroundObserver: NSObjectProtocol? = null
    private var backgroundObserver: NSObjectProtocol? = null

    /** The in-flight background task, or [UIBackgroundTaskInvalid] when none is held. */
    private var backgroundTask = UIBackgroundTaskInvalid

    actual fun observeForeground(onForeground: () -> Unit) {
        stopObserving()

        val center = NSNotificationCenter.defaultCenter

        // didBecomeActive rather than willEnterForeground: the latter also fires on the very
        // first launch before anything is bound, and it fires while the app is still
        // transitioning, when rebinding a listening socket is less reliable.
        foregroundObserver = center.addObserverForName(
            name = UIApplicationDidBecomeActiveNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            // Released first: holding it past the point of being foregrounded burns the app's
            // background budget for nothing, and the budget is shared across the whole app.
            endBackgroundTask()
            onForeground()
        }

        backgroundObserver = center.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ ->
            beginBackgroundTask()
        }
    }

    actual fun stopObserving() {
        val center = NSNotificationCenter.defaultCenter
        foregroundObserver?.let(center::removeObserver)
        backgroundObserver?.let(center::removeObserver)
        foregroundObserver = null
        backgroundObserver = null
        endBackgroundTask()
    }

    /**
     * Asks iOS to keep the app running for a short while after it is backgrounded.
     *
     * Buys roughly 30 seconds — not a way to stay connected indefinitely, but it covers the
     * common case exactly: glancing at the desktop and coming straight back, which would
     * otherwise drop the connection and force a reconnect for a two-second detour. Past the
     * window iOS suspends the app as usual and the desktop's retry loop takes over.
     */
    private fun beginBackgroundTask() {
        if (backgroundTask != UIBackgroundTaskInvalid) return
        backgroundTask = UIApplication.sharedApplication.beginBackgroundTaskWithName(
            taskName = "AlohomoraDevTools",
        ) {
            // Non-optional. iOS kills the app outright if a background task is still held when
            // its window expires, so this handler must end it no matter what.
            endBackgroundTask()
        }
    }

    private fun endBackgroundTask() {
        val task = backgroundTask
        if (task == UIBackgroundTaskInvalid) return
        // Cleared before the call: endBackgroundTask can re-enter through the expiration handler,
        // and ending the same identifier twice is a hard crash.
        backgroundTask = UIBackgroundTaskInvalid
        // Hopped to main because this is also reachable from DevToolsRuntime.stop(), which runs
        // on whatever thread toggled the server. UIApplication is main-thread only.
        onMain { UIApplication.sharedApplication.endBackgroundTask(task) }
    }

    private fun onMain(block: () -> Unit) {
        if (NSThread.isMainThread) block() else dispatch_async(dispatch_get_main_queue()) { block() }
    }
}
