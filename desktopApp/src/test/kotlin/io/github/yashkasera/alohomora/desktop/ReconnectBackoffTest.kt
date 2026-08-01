package io.github.yashkasera.alohomora.desktop

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

    @Test
    fun `reconnecting carries the target so the UI can name it`() {
        val reconnecting = DevToolsConnection.Reconnecting("192.168.0.5", 53999, attempt = 1)

        assertEquals("192.168.0.5", reconnecting.host)
        assertEquals(53999, reconnecting.port)
    }
}
