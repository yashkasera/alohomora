package io.github.yashkasera.alohomora.devtools

/**
 * No-op on Android.
 *
 * Android does not suspend a backgrounded process the way iOS does — the DevTools server keeps
 * accepting and serving while the app is in the background, so there is nothing to rebind on
 * resume. When Android *does* reclaim the process it kills it outright, and no lifecycle callback
 * survives that; recovery there is the desktop's reconnect loop plus the app being relaunched.
 *
 * Deliberately a no-op rather than an `expect` with no Android `actual`: the runtime calls this
 * unconditionally, and the reason it does nothing here is worth stating once.
 */
internal actual object AppLifecycle {
    actual fun observeForeground(onForeground: () -> Unit) = Unit
    actual fun stopObserving() = Unit
}
