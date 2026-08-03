package io.github.yashkasera.alohomora.error

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Guards the one property of the crash handler whose failure is silent and expensive: Alohomora must
 * hand the exception on to whatever handler it displaced.
 *
 * If this breaks, a debug build stops reporting crashes to Crashlytics/Sentry while release builds
 * keep working — a difference nobody attributes to a debugging library. It cannot be tested on iOS
 * (`Thread.setDefaultUncaughtExceptionHandler` has no analogue), which is why it lives here rather
 * than in `commonTest`.
 *
 * These tests invoke the installed handler directly rather than throwing for real: an actual uncaught
 * exception would take the test JVM down with it.
 */
class CrashHandlerTest {

    /** Nullable on purpose: the Gradle test JVM installs no default handler. */
    private var original: Thread.UncaughtExceptionHandler? = null

    @BeforeTest
    fun captureOriginal() {
        original = Thread.getDefaultUncaughtExceptionHandler()
        // Each test drives the handler once, and the claim is one-shot for the whole process.
        ErrorCapture.resetFatalClaimForTest()
    }

    @AfterTest
    fun restoreOriginal() {
        // Leaking our handler would route every later test's uncaught exception through Alohomora.
        Thread.setDefaultUncaughtExceptionHandler(original)
    }

    @Test
    fun installReplacesTheDefaultHandler() {
        val before = Thread.UncaughtExceptionHandler { _, _ -> }
        Thread.setDefaultUncaughtExceptionHandler(before)

        installCrashHandler()

        val installed = assertNotNull(Thread.getDefaultUncaughtExceptionHandler())
        assertNotSame(before, installed, "installCrashHandler did not take effect")
    }

    @Test
    fun theDisplacedHandlerStillReceivesTheException() {
        val seen = mutableListOf<Pair<Thread, Throwable>>()
        val previous = Thread.UncaughtExceptionHandler { thread, throwable ->
            seen += thread to throwable
        }
        Thread.setDefaultUncaughtExceptionHandler(previous)

        installCrashHandler()
        val boom = IllegalStateException("boom")
        Thread.getDefaultUncaughtExceptionHandler()!!.uncaughtException(Thread.currentThread(), boom)

        assertEquals(1, seen.size, "the displaced handler was not called exactly once")
        assertSame(boom, seen.single().second, "a different throwable was forwarded")
        assertSame(Thread.currentThread(), seen.single().first)
    }

    @Test
    fun chainingSurvivesAFailureInsideAlohomora() {
        // Koin is never started in this test, so `persistError` resolves nothing and the recording
        // path is a no-op — which is the point. Whatever goes wrong on our side, the host app's
        // handler must still run, because that call sits in a `finally`.
        var chained = false
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> chained = true }

        installCrashHandler()
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), IllegalStateException("boom"))

        assertTrue(chained, "chaining must not depend on Alohomora's own recording succeeding")
    }

    @Test
    fun aNullPreviousHandlerIsNotAFatalCase() {
        // A host app that never set one. Chaining has nothing to call and must not NPE — otherwise
        // Alohomora's handler throws while handling a crash.
        @Suppress("UNCHECKED_CAST")
        Thread.setDefaultUncaughtExceptionHandler(null)

        installCrashHandler()
        Thread.getDefaultUncaughtExceptionHandler()!!
            .uncaughtException(Thread.currentThread(), IllegalStateException("boom"))
    }
}
