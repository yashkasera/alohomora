package io.github.yashkasera.alohomora.devtools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The throttle is the only thing standing between a four-digit code and any app on the device
 * that can reach the loopback port, so these pin the arithmetic that makes guessing infeasible.
 */
class PairingThrottleTest {

    private var clock = 0L
    private val throttle = PairingThrottle(now = { clock })

    private fun failTimes(n: Int): Long {
        var last = 0L
        repeat(n) { last = throttle.recordFailure() }
        return last
    }

    @Test
    fun `free attempts do not lock`() {
        repeat(PairingThrottle.FREE_ATTEMPTS - 1) {
            assertEquals(0, throttle.recordFailure())
        }
        assertEquals(0, throttle.remainingLockoutMillis())
    }

    @Test
    fun `the attempt that exhausts the free ones starts a lockout`() {
        val lockout = failTimes(PairingThrottle.FREE_ATTEMPTS)

        assertEquals(PairingThrottle.FIRST_LOCKOUT_MILLIS, lockout)
        assertEquals(PairingThrottle.FIRST_LOCKOUT_MILLIS, throttle.remainingLockoutMillis())
    }

    @Test
    fun `the lockout expires with time`() {
        failTimes(PairingThrottle.FREE_ATTEMPTS)

        clock += PairingThrottle.FIRST_LOCKOUT_MILLIS

        assertEquals(0, throttle.remainingLockoutMillis())
    }

    @Test
    fun `each lockout doubles the next`() {
        failTimes(PairingThrottle.FREE_ATTEMPTS)
        clock += PairingThrottle.FIRST_LOCKOUT_MILLIS

        val second = failTimes(PairingThrottle.FREE_ATTEMPTS)

        assertEquals(PairingThrottle.FIRST_LOCKOUT_MILLIS * 2, second)
    }

    @Test
    fun `lockouts are capped`() {
        var lockout = 0L
        repeat(20) {
            lockout = failTimes(PairingThrottle.FREE_ATTEMPTS)
            clock += lockout
        }

        assertEquals(PairingThrottle.MAX_LOCKOUT_MILLIS, lockout)
    }

    @Test
    fun `a correct code resets failures and escalation`() {
        failTimes(PairingThrottle.FREE_ATTEMPTS)
        clock += PairingThrottle.FIRST_LOCKOUT_MILLIS
        failTimes(PairingThrottle.FREE_ATTEMPTS - 1)

        throttle.recordSuccess()

        // Back to the full allowance, and the next lockout starts from the first step again.
        assertEquals(0, failTimes(PairingThrottle.FREE_ATTEMPTS - 1))
        assertEquals(PairingThrottle.FIRST_LOCKOUT_MILLIS, throttle.recordFailure())
    }

    @Test
    fun `pairing codes are four digits`() {
        repeat(500) {
            val code = generatePairingCode()
            assertEquals(4, code.length, "code $code")
            assertTrue(code.all(Char::isDigit), "code $code")
            assertTrue(code.toInt() in 1000..9999, "code $code")
        }
    }

    @Test
    fun `pairing codes vary`() {
        // Not a randomness test. It catches a constant or a per-process seed that repeats.
        val codes = List(200) { generatePairingCode() }.toSet()

        assertTrue(codes.size > 150, "only ${codes.size} distinct codes in 200 draws")
    }
}
