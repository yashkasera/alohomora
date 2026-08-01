package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.common.AuthOtpRequiredMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.AuthSuccessMessage
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Wire-level contract for trust-on-first-use.
 *
 * The device reveals its OTP only when a client's probe carries no usable token, so the exact
 * shape of these two messages decides whether an approved desktop reconnects silently or the
 * prompt fires on every connection.
 */
class AuthHandshakeTest {

    /**
     * encodeEnvelope emits a *framed* array (9-byte header + JSON), so the matching decoder is
     * decodeFrame — decodeEnvelope takes bare payload bytes despite the symmetric name.
     */
    private fun roundTrip(message: DevToolsMessage): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeFrame(DevToolsProtocol.encodeEnvelope(message)))

    /** Decodes a raw payload as an older peer would have sent it. */
    private fun decodePayload(json: String): DevToolsMessage =
        assertNotNull(DevToolsProtocol.decodeEnvelope(json.encodeToByteArray()))

    @Test
    fun `an empty probe survives the wire as an empty otp`() {
        // This is what a desktop with no stored token sends. The device keys "show the code" off
        // otp being empty, so an encoder that dropped or defaulted the field would break the
        // prompt entirely.
        val decoded = roundTrip(AuthResponseMessage(token = null)) as AuthResponseMessage

        assertEquals("", decoded.otp)
        assertNull(decoded.token)
    }

    @Test
    fun `a token probe round-trips`() {
        val decoded = roundTrip(AuthResponseMessage(token = "abc123")) as AuthResponseMessage

        assertEquals("abc123", decoded.token)
        assertEquals("", decoded.otp, "a token probe must not look like an OTP attempt")
    }

    @Test
    fun `an otp attempt round-trips`() {
        val decoded = roundTrip(AuthResponseMessage(otp = "4242")) as AuthResponseMessage

        assertEquals("4242", decoded.otp)
        assertNull(decoded.token)
    }

    @Test
    fun `auth success carries a freshly issued token`() {
        val decoded = roundTrip(AuthSuccessMessage(sequence = 1, token = "tok")) as AuthSuccessMessage

        assertEquals("tok", decoded.token)
    }

    @Test
    fun `auth success without a token is still valid`() {
        // Returned when the client authenticated *with* a token: nothing new to hand out.
        val decoded = roundTrip(AuthSuccessMessage(sequence = 1)) as AuthSuccessMessage

        assertNull(decoded.token)
    }

    @Test
    fun `an old client's auth response still decodes`() {
        // A client built before trust-on-first-use emits no token field at all. Decoding must
        // not throw, or upgrading the desktop would lock out every older device.
        val legacy = """{"type":"AUTH_RESPONSE","sequence":0,"otp":"1234"}"""
        val decoded = decodePayload(legacy) as AuthResponseMessage

        assertEquals("1234", decoded.otp)
        assertNull(decoded.token)
    }

    @Test
    fun `an old device's auth success still decodes`() {
        val legacy = """{"type":"AUTH_SUCCESS","sequence":7}"""
        val decoded = decodePayload(legacy) as AuthSuccessMessage

        assertNull(decoded.token, "no token means fall back to prompting every time, not a crash")
        assertTrue(decoded.sequence == 7L)
    }

    @Test
    fun `the device can say it wants a code`() {
        // Without this signal the client cannot tell "still validating my token" from "type a
        // code" — both are AWAITING_AUTH. The reconnect loop then parked the device window in
        // AwaitingAuth with no input rendered and no way to recover the session.
        val decoded = roundTrip(AuthOtpRequiredMessage(sequence = 3))

        assertTrue(decoded is AuthOtpRequiredMessage)
        assertEquals(3L, decoded.sequence)
    }

    @Test
    fun `awaiting auth does not request a code until told`() {
        // The default matters: a trusted machine passes through AwaitingAuth on its way to
        // Connected, and defaulting to true would flash an auth prompt at every reconnect.
        assertFalse(DevToolsConnection.AwaitingAuth("127.0.0.1", 53999).otpRequired)
    }

    @Test
    fun `an old device that never sends the signal still connects`() {
        // Pre-signal devices go straight from the probe to AUTH_SUCCESS. Nothing should block
        // on a message that never arrives.
        val decoded = decodePayload("""{"type":"AUTH_SUCCESS","sequence":1}""") as AuthSuccessMessage

        assertNull(decoded.token)
    }
}
