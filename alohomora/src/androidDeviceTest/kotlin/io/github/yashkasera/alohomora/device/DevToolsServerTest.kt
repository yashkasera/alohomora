package io.github.yashkasera.alohomora.device

import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.AuthChallengeMessage
import io.github.yashkasera.alohomora.common.AuthOtpRequiredMessage
import io.github.yashkasera.alohomora.common.AuthResponseMessage
import io.github.yashkasera.alohomora.common.DevToolsMessage
import io.github.yashkasera.alohomora.common.DevToolsProtocol
import io.github.yashkasera.alohomora.common.EnvelopeRead
import io.github.yashkasera.alohomora.devtools.DevToolsSocket
import io.github.yashkasera.alohomora.devtools.DevToolsTcpClient
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Only loopback is ever bound — see the bind call in `DevToolsTcpServer`. */
private const val LOOPBACK = "127.0.0.1"

// One port per test. The runtime is a process-wide singleton and its listener outlives a failed
// test's teardown, so sharing a port would let one failure decide the next test's result. All of
// them are far from the 53999 default so a real desktop session attached to the device under test
// cannot collide with the run.
private const val PORT_START = 61971
private const val PORT_ACCEPT = 61973
private const val PORT_HANDSHAKE = 61975
private const val PORT_RELEASE = 61977
private const val PORT_SINGLE_CLIENT = 61979

private const val CONNECT_TIMEOUT_MILLIS = 2_000L
private const val RETRY_INTERVAL_MILLIS = 50L

/**
 * How long to keep retrying a connect after `startDevToolsServer` returns.
 *
 * Generous because `start` is optimistic: `DevToolsTcpServer.start` launches the bind onto the IO
 * dispatcher and returns `true` before the socket exists (and *still* returns true if the bind
 * fails outright). A single connect attempt straight after would race the bind.
 */
private const val BIND_TIMEOUT_MILLIS = 10_000L

/** Ceiling on one blocking read. Comfortably above `OTP_REVEAL_GRACE_MILLIS`. */
private const val READ_TIMEOUT_MILLIS = 15_000L

/** How long a rejected client is given to observe its own disconnect. */
private const val REJECTION_TIMEOUT_MILLIS = 10_000

/** Bounds a read loop so a chatty device cannot turn a missing frame into an infinite test. */
private const val MAX_FRAMES = 32

/**
 * The DevTools TCP server, driven over a real loopback socket.
 *
 * Everything here goes through the shipped transport and codec — `DevToolsTcpClient`,
 * `DevToolsProtocol.encodeEnvelope`/`readEnvelope` — rather than a hand-rolled frame. The 9-byte
 * header is documented as frozen precisely because it is the one thing two mismatched builds agree
 * on; a test that re-implemented it would keep passing after the real framing changed underneath it.
 *
 * The server is gated on `isDebugBuild`, which on Android reads the host app's `FLAG_DEBUGGABLE`.
 * A test APK is debuggable, so these tests exercise the same path a developer's debug build does.
 */
class DevToolsServerTest {

    @get:Rule(order = 0)
    val console = ConsoleTestRule()

    /**
     * `DevToolsRuntime` is a Koin singleton, and `DevToolsTcpServer.start` returns early while a
     * socket is already bound. So a server left running by an earlier test — or an earlier class —
     * would silently swallow every `startDevToolsServer` call here and leave the port unbound.
     */
    @Before
    fun stopAnyInheritedServer() {
        Alohomora.stopDevToolsServer()
    }

    @After
    fun stopServer() {
        Alohomora.stopDevToolsServer()
    }

    @Test
    fun startReportsSuccessOnADebuggableBuild() {
        assertTrue(
            Alohomora.startDevToolsServer(PORT_START),
            "startDevToolsServer returned false: isDebugBuild could not see FLAG_DEBUGGABLE",
        )
    }

    @Test
    fun theServerAcceptsALoopbackConnection() {
        assertTrue(Alohomora.startDevToolsServer(PORT_ACCEPT))

        awaitConnect(PORT_ACCEPT).use { socket ->
            assertTrue(socket.isConnected)
        }
    }

    @Test
    fun anEmptyTokenProbeIsAnsweredWithOtpRequired() {
        assertTrue(Alohomora.startDevToolsServer(PORT_HANDSHAKE))

        runBlocking {
            val socket = connectClient(PORT_HANDSHAKE)
            try {
                withTimeout(READ_TIMEOUT_MILLIS) {
                    assertNotNull(
                        socket.readUntil { it is AuthChallengeMessage },
                        "the device never sent AUTH_CHALLENGE",
                    )

                    // The probe a desktop sends when it holds no usable token. An empty `otp` is
                    // what the device keys "reveal the code" off — not a failed attempt, so the
                    // connection must survive it.
                    socket.write(
                        DevToolsProtocol.encodeEnvelope(
                            AuthResponseMessage(otp = "", token = null),
                        ),
                    )

                    assertNotNull(
                        socket.readUntil { it is AuthOtpRequiredMessage },
                        "the device never sent AUTH_OTP_REQUIRED for an empty probe",
                    )
                }
            } finally {
                socket.close()
            }
        }
    }

    @Test
    fun stoppingReleasesThePort() {
        assertTrue(Alohomora.startDevToolsServer(PORT_RELEASE))
        awaitConnect(PORT_RELEASE).use { assertTrue(it.isConnected) }

        Alohomora.stopDevToolsServer()

        // Asserted by binding the port from the test itself rather than by restarting the runtime
        // and reconnecting. Two reasons, both about what the assertion can actually prove:
        // `startDevToolsServer` returns true even when the bind fails, so a restart-then-connect
        // would report success for a port that was never released; and SO_REUSEADDR here means a
        // lingering TIME_WAIT from the connection above cannot fail this, while a *live* listener
        // still does — which is the only thing being asserted.
        awaitPortFree(PORT_RELEASE)
    }

    @Test
    fun aSecondClientIsNotServedWhileOneIsAttached() {
        assertTrue(Alohomora.startDevToolsServer(PORT_SINGLE_CLIENT))

        runBlocking {
            val incumbent = connectClient(PORT_SINGLE_CLIENT)
            try {
                // Reading the challenge is what proves the first client is *attached*, not merely
                // accepted. `attachClient` rejects on `activeConnection != null`, so without this
                // the second connect could win the race and the test would assert nothing.
                withTimeout(READ_TIMEOUT_MILLIS) {
                    assertNotNull(
                        incumbent.readUntil { it is AuthChallengeMessage },
                        "the device never sent AUTH_CHALLENGE to the first client",
                    )
                }

                awaitConnect(PORT_SINGLE_CLIENT).use { second ->
                    second.soTimeout = REJECTION_TIMEOUT_MILLIS
                    // The device closes the socket without writing, so a clean FIN reads as -1. A
                    // reset instead of a FIN is the same refusal; a *timeout* is not, and must not
                    // be quietly folded into the pass.
                    val firstByte = try {
                        second.getInputStream().read()
                    } catch (e: SocketTimeoutException) {
                        throw AssertionError(
                            "the second client was neither served nor closed within " +
                                "${REJECTION_TIMEOUT_MILLIS}ms",
                            e,
                        )
                    } catch (e: IOException) {
                        -1
                    }
                    assertEquals(
                        -1,
                        firstByte,
                        "the device served a second client while one was already attached",
                    )
                }
            } finally {
                incumbent.close()
            }
        }
    }

    /** Connects with the shipped client, retrying until the asynchronous bind has landed. */
    private suspend fun connectClient(port: Int): DevToolsSocket {
        val client = DevToolsTcpClient()
        val deadline = System.currentTimeMillis() + BIND_TIMEOUT_MILLIS
        while (true) {
            try {
                return client.connect(LOOPBACK, port, CONNECT_TIMEOUT_MILLIS)
            } catch (e: Exception) {
                if (System.currentTimeMillis() >= deadline) throw e
                delay(RETRY_INTERVAL_MILLIS)
            }
        }
    }

    /** Same retry, over a plain socket, for the tests that only care about reachability. */
    private fun awaitConnect(port: Int): Socket {
        val deadline = System.currentTimeMillis() + BIND_TIMEOUT_MILLIS
        var last: IOException? = null
        while (System.currentTimeMillis() < deadline) {
            val socket = Socket()
            try {
                socket.connect(InetSocketAddress(LOOPBACK, port), CONNECT_TIMEOUT_MILLIS.toInt())
                return socket
            } catch (e: IOException) {
                last = e
                socket.close()
                Thread.sleep(RETRY_INTERVAL_MILLIS)
            }
        }
        throw AssertionError(
            "nothing accepted a connection on $LOOPBACK:$port within ${BIND_TIMEOUT_MILLIS}ms",
            last,
        )
    }

    /** Fails unless the test can take the port for itself, i.e. no listener is left on it. */
    private fun awaitPortFree(port: Int) {
        val deadline = System.currentTimeMillis() + BIND_TIMEOUT_MILLIS
        var last: IOException? = null
        while (System.currentTimeMillis() < deadline) {
            val probe = ServerSocket()
            try {
                probe.reuseAddress = true
                probe.bind(InetSocketAddress(LOOPBACK, port))
                probe.close()
                return
            } catch (e: IOException) {
                last = e
                probe.close()
                Thread.sleep(RETRY_INTERVAL_MILLIS)
            }
        }
        throw AssertionError(
            "$LOOPBACK:$port was still held ${BIND_TIMEOUT_MILLIS}ms after stopDevToolsServer()",
            last,
        )
    }

    /**
     * Reads frames until one matches, returning null if the stream ends or the device goes quiet.
     *
     * Reads in a loop rather than asserting on the next frame because the device has two
     * independent reasons to send `AUTH_OTP_REQUIRED` — the reply to an empty probe, and the
     * grace-period reveal for clients that never probe — and interleaves the challenge ahead of
     * both.
     */
    private suspend fun DevToolsSocket.readUntil(
        predicate: (DevToolsMessage) -> Boolean,
    ): DevToolsMessage? {
        var framesRead = 0
        while (framesRead < MAX_FRAMES) {
            framesRead++
            val read = DevToolsProtocol.readEnvelope(this)
            if (read !is EnvelopeRead.Message) return null
            if (predicate(read.message)) return read.message
        }
        return null
    }
}
