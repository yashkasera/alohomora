package io.github.yashkasera.alohomora.replay

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.HeaderRedaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

/**
 * The replay strip policy and where it is enforced.
 *
 * Lives in `:alohomora` rather than `alohomora-common` because the part that matters is device-side:
 * the desktop prepares a replay without ever seeing the app's strip list, so only
 * [TrafficReplayRegistry] re-applying it makes `Alohomora.setReplayStrippedHeaders` mean anything
 * for a desktop replay.
 *
 * Both registry and strip list are process-global, so every test starts from a clean slate.
 */
class ReplayStrippedHeadersTest {

    @BeforeTest
    fun reset() {
        TrafficReplayRegistry.clear()
        ReplayHeaders.additionalStripList = emptySet()
    }

    @AfterTest
    fun leaveNoHandlerBehind() {
        // A fake handler left registered would make isReplaySupported true for whatever runs next.
        TrafficReplayRegistry.clear()
        ReplayHeaders.additionalStripList = emptySet()
    }

    @Test
    fun `client negotiated encoding headers are always stripped`() {
        val sanitized = ReplayHeaders.sanitize(
            mapOf(
                "Accept-Encoding" to listOf("gzip"),
                "content-encoding" to listOf("gzip"),
                "Accept" to listOf("application/json"),
            ),
        )

        assertEquals(setOf("Accept"), sanitized.keys)
    }

    @Test
    fun `the public api feeds the strip list case-insensitively`() {
        Alohomora.setReplayStrippedHeaders("X-Signature", "x-nonce")

        val sanitized = ReplayHeaders.sanitize(
            mapOf(
                "x-signature" to listOf("stale"),
                "X-NONCE" to listOf("n-1"),
                "Accept" to listOf("*/*"),
            ),
        )

        assertEquals(setOf("Accept"), sanitized.keys)
    }

    @Test
    fun `each call replaces the previous list`() {
        Alohomora.setReplayStrippedHeaders("X-Signature")
        Alohomora.setReplayStrippedHeaders("X-Nonce")

        assertEquals(setOf("X-Nonce"), ReplayHeaders.additionalStripList)

        Alohomora.setReplayStrippedHeaders()

        assertEquals(emptySet(), ReplayHeaders.additionalStripList)
    }

    @Test
    fun `the registry sanitizes headers before the handler sees them`() = runTest {
        // Simulates a desktop replay: built in a process that never saw the app's strip list, so
        // X-Signature is still present and only the device-side pass can remove it.
        var received: ReplayRequest? = null
        TrafficReplayRegistry.register { request ->
            received = request
            ReplayOutcome.Sent()
        }
        Alohomora.setReplayStrippedHeaders("X-Signature")

        val outcome = TrafficReplayRegistry.replay(
            ReplayRequest(
                sourceTraceId = "t",
                method = "POST",
                url = "https://api.example.com/v1/pay",
                headers = mapOf(
                    "X-Signature" to listOf("stale"),
                    "Accept-Encoding" to listOf("gzip"),
                    "Authorization" to listOf(HeaderRedaction.REDACTED),
                    "Accept" to listOf("application/json"),
                ),
                body = "{}",
            ),
        )

        assertIs<ReplayOutcome.Sent>(outcome)
        val request = assertNotNull(received)
        assertEquals(mapOf("Accept" to listOf("application/json")), request.headers)
        assertEquals("{}", request.body, "only headers are touched")
    }
}
