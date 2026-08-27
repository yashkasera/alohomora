package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.DevToolsHeartbeat
import io.github.yashkasera.alohomora.common.DevToolsLiveness
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.PingMessage
import io.github.yashkasera.alohomora.common.PongMessage
import io.github.yashkasera.alohomora.common.ServerShuttingDownMessage
import io.github.yashkasera.alohomora.common.UnknownMessage
import io.github.yashkasera.alohomora.desktop.data.devtools.nextReconnectAttempt
import io.github.yashkasera.alohomora.desktop.data.devtools.reconnectDelayMillis
import io.github.yashkasera.alohomora.desktop.data.devtools.shouldDropSilentDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The liveness probe that lets the device reclaim its single connection slot.
 *
 * The bug being protected against: through an `adb forward` the device's socket is to the
 * on-device adb daemon, and it stays healthy after the desktop process at the far end of the USB
 * transport is gone. So killing the desktop app did not always reach the device — the device kept
 * `activeConnection` non-null and rejected every later client until the app was restarted, while
 * `lsof` on the host showed only the adb listener with nothing established.
 *
 * Nothing at the socket layer can fix that. A read timeout was tried and reverted, because after
 * auth the desktop legitimately sends nothing for minutes while it only receives streams. TCP
 * keepalive would probe a link that is genuinely up. Only an end-to-end round trip tells the two
 * apart, which is what these messages carry.
 */
class HeartbeatTest {

    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    // ── wire contract ─────────────────────────────────────────────────────────

    @Test
    fun `a ping round-trips`() {
        val decoded = roundTrip(PingMessage(sequence = 12))

        assertTrue(decoded is PingMessage)
        assertEquals(12L, decoded.sequence)
    }

    @Test
    fun `a pong echoes the ping it answers`() {
        val decoded = roundTrip(PongMessage(sequence = 12)) as PongMessage

        assertEquals(12L, decoded.sequence)
    }

    @Test
    fun `a server-shutting-down frame round-trips`() {
        // The frame that tells the desktop a disconnect was deliberate, so it stops reconnecting.
        // A rename on either side silently reverts to infinite retry, which is what this guards.
        val decoded = roundTrip(ServerShuttingDownMessage(sequence = 7))

        assertTrue(decoded is ServerShuttingDownMessage)
        assertEquals(7L, decoded.sequence)
    }

    @Test
    fun `an older desktop ignores a server-shutting-down frame instead of dropping`() {
        // A desktop predating this frame must not choke on it: it decodes to UnknownMessage and is
        // skipped, leaving that older desktop on its previous retry behaviour rather than crashing
        // the read loop. The current build knows the real type, so an older peer can only be
        // simulated with a name it has never seen.
        val decoded = decodePayload("""{"type":"SERVER_STOPPING_LEGACY","sequence":7}""")

        assertTrue(decoded is UnknownMessage)
    }

    @Test
    fun `a client must opt in before the device will ping it`() {
        // The whole safety property of the rollout. A client that predates PING ignores it as an
        // unknown type, which the device cannot distinguish from a dead peer — so the device must
        // never ping a client that has not promised to answer.
        val legacy = """{"type":"AUTH_RESPONSE","sequence":0,"otp":"","token":"tok"}"""
        val decoded = decodePayload(legacy) as AuthResponseMessage

        assertFalse(
            decoded.heartbeatSupported,
            "an older client's probe must not be read as heartbeat-capable",
        )
    }

    @Test
    fun `the opt-in survives the wire`() {
        val decoded = roundTrip(
            AuthResponseMessage(token = "tok", heartbeatSupported = true),
        ) as AuthResponseMessage

        assertTrue(decoded.heartbeatSupported)
        assertEquals("tok", decoded.token)
    }

    @Test
    fun `an older peer sees a ping as an ignorable unknown type`() {
        // Stands in for a desktop built before PING existed: the sealed hierarchy maps unseen
        // @SerialNames to UnknownMessage, which every handler skips. Without that, a newer device
        // pinging an older desktop would throw out of its read loop and kill the session — turning
        // a fix for a wedged connection into a disconnect bug.
        val decoded = decodePayload("""{"type":"SOME_FUTURE_PROBE","sequence":4}""")

        assertTrue(decoded is UnknownMessage)
    }

    // ── silence policy ────────────────────────────────────────────────────────

    /** Drives [DevToolsLiveness] off a hand-cranked clock instead of waiting out real time. */
    private class FakeClock {
        var millis: Long = 0
        fun reader(): () -> Long = { millis }
    }

    @Test
    fun `a fresh peer is not silent`() {
        val clock = FakeClock()
        val liveness =
            DevToolsLiveness(silenceTimeoutMillis = 20_000, elapsedMillis = clock.reader())

        assertFalse(liveness.isPeerSilent())
    }

    @Test
    fun `an answering peer never goes silent no matter how long the session runs`() {
        val clock = FakeClock()
        val liveness =
            DevToolsLiveness(silenceTimeoutMillis = 20_000, elapsedMillis = clock.reader())

        // A well-behaved peer pongs every ping. This is the case a read timeout got wrong.
        repeat(500) {
            clock.millis += DevToolsHeartbeat.PING_INTERVAL_MILLIS
            liveness.recordSignOfLife()
            assertFalse(liveness.isPeerSilent(), "a peer answering every ping must survive")
        }
    }

    @Test
    fun `a peer that stops answering is condemned, but not on the first missed interval`() {
        val clock = FakeClock()
        val liveness =
            DevToolsLiveness(silenceTimeoutMillis = 20_000, elapsedMillis = clock.reader())

        // One dropped or delayed frame must not end a healthy session.
        clock.millis = 5_000
        assertFalse(liveness.isPeerSilent())
        clock.millis = 20_000
        assertFalse(liveness.isPeerSilent(), "the window is inclusive of the timeout itself")

        clock.millis = 20_001
        assertTrue(liveness.isPeerSilent())
    }

    @Test
    fun `a late reply rescues a peer that was nearly condemned`() {
        val clock = FakeClock()
        val liveness =
            DevToolsLiveness(silenceTimeoutMillis = 20_000, elapsedMillis = clock.reader())

        clock.millis = 19_000
        liveness.recordSignOfLife()
        clock.millis = 30_000

        assertFalse(liveness.isPeerSilent(), "the window is silence-since-last-frame, not uptime")
    }

    @Test
    fun `the default window spans several pings`() {
        // If the window were one interval, a single scheduling hiccup on either side would end the
        // session. If it were minutes, the wedged slot this exists to free would outlast the
        // developer's patience and they would restart the app anyway.
        val intervals =
            DevToolsHeartbeat.SILENCE_TIMEOUT_MILLIS / DevToolsHeartbeat.PING_INTERVAL_MILLIS

        assertTrue(intervals >= 3, "too tight: a single missed frame would drop a healthy session")
        assertTrue(
            DevToolsHeartbeat.SILENCE_TIMEOUT_MILLIS <= 60_000,
            "too slack: the connection slot must come back in seconds, not minutes",
        )
    }

    // ── desktop watchdog gate ─────────────────────────────────────────────────

    @Test
    fun `an unarmed watchdog never drops a device no matter how silent`() {
        // A device on an SDK predating PING sends none, so the desktop never arms — and silence
        // from it must not be read as death. Dropping it would reintroduce the read-timeout bug
        // this whole mechanism replaced, this time on the desktop side.
        assertFalse(shouldDropSilentDevice(armed = false, peerSilent = true))
        assertFalse(shouldDropSilentDevice(armed = false, peerSilent = false))
    }

    @Test
    fun `an armed watchdog drops only once the device is actually silent`() {
        // Armed by the first PING. Now silence is evidence: a pinging device that stops has died
        // without a FIN, exactly the case the parked read loop can never notice on its own.
        assertTrue(shouldDropSilentDevice(armed = true, peerSilent = true))
        assertFalse(shouldDropSilentDevice(armed = true, peerSilent = false))
    }

    // ── backoff reset ─────────────────────────────────────────────────────────

    @Test
    fun `a dropped long-lived session retries immediately, not at the cap`() {
        // The counter used to live outside the retry loop and never reset. After a session that had
        // been up for an hour, the reconnect sat at the 5s cap — the slowest retry for the case
        // most likely to succeed at once.
        val afterManyRetries = 12

        assertEquals(1, nextReconnectAttempt(afterManyRetries, sessionEstablished = true))
        assertEquals(500L, reconnectDelayMillis(nextReconnectAttempt(afterManyRetries, true)))
    }

    @Test
    fun `a device that never completes the handshake still backs off`() {
        // A bare TCP connect is not proof of life: an iOS app the OS suspended still has its socket
        // bound, so the connect completes from the kernel's listen backlog and then hangs. Resetting
        // on connect rather than on AUTH_SUCCESS would hammer such a device twice a second forever.
        var attempt = 0
        repeat(6) { attempt = nextReconnectAttempt(attempt, sessionEstablished = false) }

        assertEquals(6, attempt)
        assertEquals(5_000L, reconnectDelayMillis(attempt))
    }
}
