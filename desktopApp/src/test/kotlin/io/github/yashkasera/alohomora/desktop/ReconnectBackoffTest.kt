package io.github.yashkasera.alohomora.desktop

import io.github.yashkasera.alohomora.desktop.data.devtools.nextReconnectAttempt
import io.github.yashkasera.alohomora.desktop.data.devtools.reconnectDelayMillis
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reconnect pacing and state.
 *
 * The behaviour being protected: an iOS app suspended in the background stops servicing its
 * socket, which from the desktop is indistinguishable from a crash. The old code dropped straight
 * to Disconnected and the user had to reconnect by hand every time they glanced at their phone.
 */
class ReconnectBackoffTest {

    /** The real function the reconnect loop calls. */
    private fun delayFor(attempt: Int): Long = reconnectDelayMillis(attempt)

    @Test
    fun `the first retry is quick`() {
        // The common case is a foreground transition, which resolves in well under a second.
        // A slow first retry would make every app-switch feel broken.
        assertEquals(500L, delayFor(1))
    }

    @Test
    fun `backoff grows then plateaus`() {
        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L), (1..4).map(::delayFor))

        // Capped, not unbounded: a device that is genuinely gone should still be picked up
        // within seconds of coming back, not after a doubling sequence has run away.
        assertTrue((5..50).all { delayFor(it) == 5_000L })
    }

    @Test
    fun `delays never regress as attempts climb`() {
        val delays = (1..20).map(::delayFor)
        assertEquals(delays.sorted(), delays)
    }

    @Test
    fun `successive attempts are not equal`() {
        // MutableStateFlow drops an emission equal to the current value. If Reconnecting did not
        // carry the attempt, every retry after the first would be silently swallowed and the UI
        // would freeze on "reconnecting (1)" forever while the loop span underneath.
        val first = DevToolsConnection.Reconnecting("127.0.0.1", 53999, attempt = 1)
        val second = DevToolsConnection.Reconnecting("127.0.0.1", 53999, attempt = 2)

        assertTrue(first != second, "state must change per attempt or StateFlow will dedupe it")
    }

    /**
     * Replays the retry loop's counter update verbatim: `attempt = nextReconnectAttempt(attempt,
     * established)` on each drop, then `reconnectDelayMillis(attempt)` for the wait. Both are the
     * real functions the loop calls, so this guards the composed behaviour the loop depends on.
     */
    private fun delaysFor(drops: List<Boolean>): List<Long> {
        var attempt = 0
        return drops.map { established ->
            attempt = nextReconnectAttempt(attempt, sessionEstablished = established)
            delayFor(attempt)
        }
    }

    @Test
    fun `an established session that drops resets to the quick first retry`() {
        // The whole point of the fix: after a session that reached AUTH_SUCCESS drops, the backoff
        // starts over at 500ms instead of resuming wherever the counter had climbed to. A
        // never-established flaky session (established = false) keeps climbing to the 5s cap.
        val delays = delaysFor(
            listOf(false, false, false, false, false) + // never connected: climb to the cap
                true, // a session that actually connected then dropped
        )

        assertEquals(listOf(500L, 1_000L, 2_000L, 4_000L, 5_000L, 500L), delays)
    }

    @Test
    fun `an established drop resets even from the plateau, then climbs again`() {
        // Model a long-lived session: many never-established retries park at the cap, one
        // established drop snaps back to 500ms, and subsequent never-established drops climb anew.
        val delays = delaysFor(
            List(12) { false } + listOf(true, false, false),
        )

        assertEquals(5_000L, delays[11], "parked at the cap before the established drop")
        assertEquals(500L, delays[12], "established drop resets to the quick retry")
        assertEquals(listOf(1_000L, 2_000L), delays.subList(13, 15))
    }

    @Test
    fun `reconnecting carries the target so the UI can name it`() {
        val reconnecting = DevToolsConnection.Reconnecting("192.168.0.5", 53999, attempt = 1)

        assertEquals("192.168.0.5", reconnecting.host)
        assertEquals(53999, reconnecting.port)
    }
}
