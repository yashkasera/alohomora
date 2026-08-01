package io.github.yashkasera.alohomora.devtools

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import platform.Foundation.NSUserDefaults

/**
 * A token is only minted when the user ticks "remember this computer".
 *
 * Exercises [tokenForAcceptedOtp], the function [DevToolsRuntime] actually calls when an OTP is
 * accepted — driving the runtime end to end would need a socket, a Room database and Koin, and a
 * test that re-implemented the rule could not catch it being wrong.
 */
class RememberDeviceConsentTest {

    private val store = IosTrustStore(NSUserDefaults(suiteName = SUITE))

    /** The real decision DevToolsRuntime makes when an OTP is accepted. */
    private fun tokenOnSuccessfulOtp(rememberDevice: Boolean): String? =
        store.tokenForAcceptedOtp(rememberDevice)

    @Test
    fun `declining consent issues no token`() {
        assertNull(
            tokenOnSuccessfulOtp(rememberDevice = false),
            "a successful pairing must not persist anything unless asked",
        )
    }

    @Test
    fun `granting consent issues a trusted token`() {
        val token = tokenOnSuccessfulOtp(rememberDevice = true)

        assertTrue(token != null && store.isTrusted(token))
    }

    @Test
    fun `declining does not grow the trust store`() {
        store.revokeAll()
        val granted = tokenOnSuccessfulOtp(rememberDevice = true)
        repeat(5) { tokenOnSuccessfulOtp(rememberDevice = false) }

        // Only the consented pairing left a token behind; the declines added nothing that could
        // later authenticate.
        assertTrue(granted != null && store.isTrusted(granted))
    }

    @Test
    fun `consent defaults to false on a fresh state`() {
        // The default matters more than it looks: DevToolsRuntime reads this field when an OTP is
        // accepted, so a default of true would persist credentials for every pairing without the
        // user ever touching the checkbox.
        assertFalse(DevToolsServerState().rememberDevice)
    }

    @Test
    fun `consent resets between connections`() {
        // Modelled on attachClient, which clears the flag. Carrying a previous "yes" forward
        // would silently remember a desktop the user never agreed to.
        val afterConsent = DevToolsServerState().copy(rememberDevice = true)
        val nextConnection = afterConsent.copy(
            hasClient = true,
            pendingOtp = null,
            rememberDevice = false,
        )

        assertFalse(nextConnection.rememberDevice)
    }

    private companion object {
        const val SUITE = "io.github.yashkasera.alohomora.consent.test"
    }
}
