package io.github.yashkasera.alohomora.devtools

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Throttles pairing-code guesses across every connection the server accepts.
 *
 * The code stays four digits because the desktop's input accepts exactly four, so a longer code
 * would lock out every desktop already installed. That leaves 9000 codes, and a per-connection
 * limit cannot protect them: a fresh code is drawn per connection and reconnecting is free. The
 * loopback bind keeps the LAN out but not other apps on the same Android device, any of which can
 * reach the port with nothing beyond INTERNET permission. So the limit lives here, outside any one
 * connection: after [FREE_ATTEMPTS] consecutive wrong codes every guess is refused unchecked, for a
 * lockout that doubles each time up to [MAX_LOCKOUT_MILLIS]. That moves an exhaustive guess from
 * minutes to weeks. Only a correct code resets it.
 *
 * Trust-token logins never consult this. A 256-bit token cannot be guessed, and an app able to trip
 * the lockout must not be able to lock a known desktop out with it.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class PairingThrottle(
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {

    private data class State(
        val consecutiveFailures: Int = 0,
        val lockouts: Int = 0,
        val lockedUntil: Long = 0,
    )

    // Swapped whole by CAS rather than mutated field by field: sockets are accepted on a
    // multi-threaded dispatcher, so two connections can report a failure at the same moment.
    private val state = AtomicReference(State())

    /** Milliseconds until a code will be checked again; 0 when one may be checked now. */
    fun remainingLockoutMillis(): Long = (state.load().lockedUntil - now()).coerceAtLeast(0)

    /**
     * Records a wrong code. Returns the lockout this failure started, in milliseconds, or 0 while
     * free attempts remain.
     */
    fun recordFailure(): Long {
        while (true) {
            val current = state.load()
            val failures = current.consecutiveFailures + 1
            if (failures < FREE_ATTEMPTS) {
                if (state.compareAndSet(current, current.copy(consecutiveFailures = failures))) return 0
            } else {
                val lockout = lockoutMillis(current.lockouts)
                val next = State(lockouts = current.lockouts + 1, lockedUntil = now() + lockout)
                if (state.compareAndSet(current, next)) return lockout
            }
        }
    }

    /** A correct code: the next mistake starts from a clean slate. */
    fun recordSuccess() {
        state.store(State())
    }

    private fun lockoutMillis(previousLockouts: Int): Long =
        (FIRST_LOCKOUT_MILLIS shl previousLockouts.coerceAtMost(MAX_DOUBLINGS))
            .coerceAtMost(MAX_LOCKOUT_MILLIS)

    companion object {
        const val FREE_ATTEMPTS: Int = 3
        const val FIRST_LOCKOUT_MILLIS: Long = 30_000
        const val MAX_LOCKOUT_MILLIS: Long = 15 * 60_000

        // 30 s doubled five times passes the cap; shifting further would only overflow.
        private const val MAX_DOUBLINGS = 5
    }
}

/**
 * Draws a four-digit pairing code from the platform CSPRNG.
 *
 * `kotlin.random.Random` is not cryptographically secure, so its output can be predicted from
 * earlier output. [Uuid.random] uses SecureRandom on Android and the system CSPRNG on Darwin.
 * Bytes 0-3 of a v4 UUID carry no version or variant bits, so all 32 are random.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun generatePairingCode(): String {
    val bytes = Uuid.random().toByteArray()
    val n = (0..3).fold(0u) { acc, i -> (acc shl 8) or bytes[i].toUByte().toUInt() }
    return (1000u + n % 9000u).toString()
}
