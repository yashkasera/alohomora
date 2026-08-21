package io.github.yashkasera.alohomora.error

import io.github.yashkasera.alohomora.common.Error
import kotlin.concurrent.Volatile
import kotlin.time.Clock

/**
 * Shared plumbing for turning a [Throwable] into an [Error] row, and the one piece of state the
 * platform crash handlers need to agree on.
 *
 * The handlers themselves are `expect`/`actual` because the hook differs per platform and because
 * persisting from a dying process needs `runBlocking`, which is not available in `commonMain`.
 */
internal object ErrorCapture {

    /**
     * Ceiling on how long a crashing thread may block writing the error.
     *
     * Persisting has to be synchronous — the process is about to die and a `scope.launch` would
     * never be scheduled — but a SQLite write can block on a lock another thread holds, and this
     * runs on the thread that is already crashing. Without a bound, losing the race turns a crash
     * into a hang, which is strictly worse than losing the record: the user gets an ANR and no
     * stack trace instead of a stack trace in the console.
     */
    const val FATAL_TIMEOUT_MILLIS = 2_000L

    @Volatile
    private var handlingFatal = false

    /**
     * Claims the right to handle a fatal, returning false if one is already being handled.
     *
     * A crash raised *by* the crash handler would otherwise re-enter it forever. Never reset:
     * the process is terminating either way, so there is no second fatal worth recording.
     */
    fun claimFatal(): Boolean {
        if (handlingFatal) return false
        handlingFatal = true
        return true
    }

    /**
     * Test-only. Production never resets this.
     *
     * The claim is process-global and one-shot, so without a reset the first test to record a fatal
     * silently disarms every later one — which is how it behaved before this existed: a crash-handler
     * test and a guard test in the same JVM could not both pass, and the order decided which.
     */
    fun resetFatalClaimForTest() {
        handlingFatal = false
    }

    fun toError(throwable: Throwable, place: String?): Error = Error(
        place = place ?: throwable.topFrame(),
        reason = throwable.describe(),
        stackTrace = throwable.stackTraceToString(),
        time = Clock.System.now().toEpochMilliseconds(),
    )

    /**
     * `SimpleName: message`, which is what both error screens parse back out via
     * [exceptionTypeName].
     *
     * Deliberately `simpleName`, not `qualifiedName`: the latter is not universally supported on
     * Kotlin/Native, and a reflection failure *inside a crash handler* would replace the app's real
     * crash with ours. Nothing displays the package anyway, and `stackTrace` carries it.
     */
    private fun Throwable.describe(): String {
        val type = this::class.simpleName ?: "Throwable"
        return message?.takeIf { it.isNotBlank() }?.let { "$type: $it" } ?: type
    }

    /** First `at …` frame, as a human-readable location. Null when the trace has no frames. */
    private fun Throwable.topFrame(): String? = stackTraceToString()
        .lineSequence()
        .map { it.trim() }
        .firstOrNull { it.startsWith("at ") }
        ?.removePrefix("at ")
}
