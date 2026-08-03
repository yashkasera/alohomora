package io.github.yashkasera.alohomora.error

import co.touchlab.kermit.Logger
import io.github.yashkasera.alohomora.Alohomora
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.setUnhandledExceptionHook
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The hook Alohomora displaced, kept so the chain survives.
 *
 * A file-level `var` rather than a local, because [setUnhandledExceptionHook] returns the previous
 * hook and the lambda being installed has to read it — a local `val` cannot be referenced inside the
 * expression that initializes it. The assignment lands before any exception can fire the hook.
 */
private var previousHook: ((Throwable) -> Unit)? = null

@OptIn(ExperimentalNativeApi::class)
internal actual fun installCrashHandler() {
    previousHook = setUnhandledExceptionHook { throwable ->
        try {
            if (ErrorCapture.claimFatal()) {
                recordFatal(throwable)
            }
        } finally {
            previousHook?.invoke(throwable)
        }
        // Returning normally is enough — the runtime terminates with the unhandled exception once
        // the hook completes. Calling terminateWithUnhandledException here would report it twice.
    }
}

/**
 * Covers **Kotlin** exceptions only.
 *
 * An Obj-C `NSException` or a Swift `fatalError` never reaches this hook; those need
 * `NSSetUncaughtExceptionHandler` / a signal handler on the Swift side, which Alohomora deliberately
 * does not install — competing for those handlers is how a debug library breaks a host app's own
 * crash reporter. Swift code with a caught failure should call `Alohomora.recordError(reason:…)`.
 */
private fun recordFatal(throwable: Throwable) {
    try {
        runBlocking {
            withTimeoutOrNull(ErrorCapture.FATAL_TIMEOUT_MILLIS.milliseconds) {
                Alohomora.persistError(ErrorCapture.toError(throwable, place = null))
            } ?: Logger.d { "[Alohomora] timed out persisting fatal; crash not recorded" }
        }
    } catch (e: Throwable) {
        Logger.d { "[Alohomora] failed to persist fatal: ${e.message}" }
    }
}
