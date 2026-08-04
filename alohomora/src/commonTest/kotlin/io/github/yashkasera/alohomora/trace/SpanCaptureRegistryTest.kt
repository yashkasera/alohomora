package io.github.yashkasera.alohomora.trace

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Guards the one-way latch on [SpanCaptureRegistry].
 *
 * It exists because Alohomora depends on no tracing SDK, so nothing can be inspected to find out
 * whether spans will ever arrive. The desktop uses this to tell "this app has no tracer" from "no
 * traces yet", and to avoid sending `REQUEST_TRACE_SPANS` to a device that will never answer.
 *
 * Deliberately no reset: a process-global latch with a test-only reset would let a future change quietly
 * flip it back to false, which would make the desktop hide a panel that already has data in it. That is
 * also why this test only asserts the forward direction — there is nothing else to assert.
 */
class SpanCaptureRegistryTest {

    @Test
    fun `markActive latches on and is idempotent`() {
        SpanCaptureRegistry.markActive()
        assertTrue(SpanCaptureRegistry.isSupported)

        SpanCaptureRegistry.markActive()
        assertTrue(SpanCaptureRegistry.isSupported)
    }
}
