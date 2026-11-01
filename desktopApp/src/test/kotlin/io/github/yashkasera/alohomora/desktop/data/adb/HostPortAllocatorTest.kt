package io.github.yashkasera.alohomora.desktop.data.adb

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Host-port allocation for `adb forward`. Every Android session used to get 53999, so the second
 * device's forward took over the first one's tunnel.
 */
class HostPortAllocatorTest {

    private val allFree: (Int) -> Boolean = { true }

    @Test
    fun `a single device keeps the default port`() {
        assertEquals(53999, HostPortAllocator.allocate(reserved = emptySet(), isFree = allFree))
    }

    @Test
    fun `a second device gets the next port rather than sharing the first one's`() {
        assertEquals(54000, HostPortAllocator.allocate(reserved = setOf(53999), isFree = allFree))
    }

    @Test
    fun `ports held outside this process are skipped`() {
        // An iOS Simulator app on the host's 53999, or a forward left by another instance.
        val taken = setOf(53999, 54000)
        val port = HostPortAllocator.allocate(reserved = emptySet(), isFree = { it !in taken })
        assertEquals(54001, port)
    }

    @Test
    fun `a reserved port is skipped even when the probe reports it free`() {
        // A window whose device dropped offline: adb has released the listener, but the window
        // will re-forward the same port when the device returns.
        val port = HostPortAllocator.allocate(reserved = setOf(53999, 54001), isFree = allFree)
        assertEquals(54000, port)
    }

    @Test
    fun `an exhausted range yields null rather than a taken port`() {
        assertNull(HostPortAllocator.allocate(reserved = emptySet(), span = 3, isFree = { false }))
        assertNull(
            HostPortAllocator.allocate(reserved = setOf(53999, 54000), span = 2, isFree = allFree),
        )
    }

    @Test
    fun `the search never runs past the last valid port`() {
        val probed = mutableListOf<Int>()
        val port = HostPortAllocator.allocate(
            reserved = emptySet(),
            preferred = 65534,
            span = 10,
            isFree = { probed += it; false },
        )
        assertNull(port)
        assertEquals(listOf(65534, 65535), probed)
    }

    @Test
    fun `the loopback probe sees a live listener`() {
        ServerSocket().use { socket ->
            socket.bind(InetSocketAddress(InetAddress.getLoopbackAddress(), 0))
            val port = socket.localPort
            assertFalse(HostPortAllocator.isLoopbackPortFree(port))
            socket.close()
            assertTrue(HostPortAllocator.isLoopbackPortFree(port))
        }
    }
}
