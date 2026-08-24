package io.github.yashkasera.alohomora.error

import co.touchlab.kermit.Logger
import io.github.yashkasera.alohomora.AlohomoraImpl
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

internal actual fun installCrashHandler() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()

    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        // Record first, chain second. If our write throws or times out the app still has to crash
        // the way it would have without Alohomora installed, so the chain call is in a `finally`.
        try {
            if (ErrorCapture.claimFatal()) {
                recordFatal(throwable, place = "thread ${thread.name}")
            }
        } finally {
            // No fallback to `ThreadGroup.uncaughtException` when `previous` is null: the platform
            // default is already the thread's group, and invoking it here would double-report.
            previous?.uncaughtException(thread, throwable)
        }
    }
}

/**
 * Persists synchronously on the crashing thread.
 *
 * `runBlocking` is unavoidable: the process is one stack unwind from death, so anything handed to a
 * background dispatcher is never scheduled. [ErrorCapture.FATAL_TIMEOUT_MILLIS] is what keeps that
 * from turning a crash into an ANR when the write blocks on a lock another thread holds.
 */
private fun recordFatal(throwable: Throwable, place: String) {
    try {
        runBlocking {
            withTimeoutOrNull(ErrorCapture.FATAL_TIMEOUT_MILLIS.milliseconds) {
                AlohomoraImpl.persistError(ErrorCapture.toError(throwable, place))
            } ?: Logger.d { "[Alohomora] timed out persisting fatal; crash not recorded" }
        }
    } catch (e: Throwable) {
        // Swallowed on purpose. Throwing out of an uncaught-exception handler replaces the app's
        // real crash with ours, and the stack trace the developer needs is the one we were handed.
        Logger.d { "[Alohomora] failed to persist fatal: ${e.message}" }
    }
}
