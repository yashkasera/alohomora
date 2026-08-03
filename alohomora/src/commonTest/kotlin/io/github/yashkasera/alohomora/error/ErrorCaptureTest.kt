package io.github.yashkasera.alohomora.error

import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.exceptionTypeName
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ErrorCaptureTest {

    @BeforeTest
    fun resetFatalClaim() {
        // The claim is process-global and one-shot, so any test that ran a crash handler first
        // would otherwise decide this class's result. See CrashHandlerTest.
        ErrorCapture.resetFatalClaimForTest()
    }

    @Test
    fun reasonIsTypeThenMessage() {
        val error = ErrorCapture.toError(IllegalStateException("cart was empty"), place = null)

        assertEquals("IllegalStateException: cart was empty", error.reason)
        assertEquals("IllegalStateException", error.exceptionTypeName())
    }

    @Test
    fun reasonOmitsTheSeparatorWhenThereIsNoMessage() {
        // "IllegalStateException: null" would be displayed verbatim in the detail view.
        val error = ErrorCapture.toError(IllegalStateException(), place = null)

        assertEquals("IllegalStateException", error.reason)
        assertEquals("IllegalStateException", error.exceptionTypeName())
    }

    @Test
    fun reasonOmitsTheSeparatorForABlankMessage() {
        val error = ErrorCapture.toError(IllegalStateException("   "), place = null)

        assertEquals("IllegalStateException", error.reason)
    }

    /**
     * The regression that made this helper worth extracting. Both error screens used to run
     * `substringAfterLast(".")` *before* `substringBefore(":")`, so a message ending in a period
     * reduced the title to the empty string and the row rendered blank.
     */
    @Test
    fun titleSurvivesAPeriodInTheMessage() {
        val error = ErrorCapture.toError(
            IllegalStateException("Config missing. Retry after init."),
            place = null,
        )

        assertEquals("IllegalStateException", error.exceptionTypeName())
    }

    @Test
    fun titleSurvivesAColonInTheMessage() {
        val error = Error(reason = "DecodingError: keyNotFound: \"id\"")

        assertEquals("DecodingError", error.exceptionTypeName())
    }

    @Test
    fun titleStripsAPackageQualifiedType() {
        // What the string overload receives when a Swift or JVM caller passes a full type name.
        val error = Error(reason = "java.lang.NullPointerException: boom")

        assertEquals("NullPointerException", error.exceptionTypeName())
    }

    @Test
    fun titleFallsBackWhenReasonIsMissingOrUnusable() {
        assertEquals("Unknown Exception", Error(reason = null).exceptionTypeName())
        assertEquals("Unknown Exception", Error(reason = "").exceptionTypeName())
        // A reason that is nothing but a separator leaves no type behind.
        assertEquals("Unknown Exception", Error(reason = ": boom").exceptionTypeName())
    }

    @Test
    fun stackTraceIsCaptured() {
        val error = ErrorCapture.toError(IllegalStateException("boom"), place = null)

        val trace = assertNotNull(error.stackTrace)
        assertTrue(trace.contains("IllegalStateException"), "trace was: $trace")
    }

    @Test
    fun explicitPlaceWinsOverTheTopFrame() {
        val error = ErrorCapture.toError(IllegalStateException("boom"), place = "SyncWorker")

        assertEquals("SyncWorker", error.place)
    }

    @Test
    fun timeIsInMilliseconds() {
        val error = ErrorCapture.toError(IllegalStateException("boom"), place = null)

        // Seconds here would sort every recorded error to the bottom of a newest-first list,
        // which is exactly what the Error entity's own default comment warns about.
        assertTrue(error.time > 1_000_000_000_000L, "time was: ${error.time}")
    }

    /**
     * Deliberately the only test that touches the guard: it is never reset, because in production
     * the process is terminating and there is no second fatal worth recording.
     */
    @Test
    fun fatalCanOnlyBeClaimedOnce() {
        assertTrue(ErrorCapture.claimFatal(), "first claim should win")
        assertFalse(ErrorCapture.claimFatal(), "re-entrant claim must be refused")
        assertFalse(ErrorCapture.claimFatal())
    }
}
