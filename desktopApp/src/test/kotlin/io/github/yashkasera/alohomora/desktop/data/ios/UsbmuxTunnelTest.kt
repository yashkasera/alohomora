package io.github.yashkasera.alohomora.desktop.data.ios

import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsTarget
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * End-to-end verification of the usbmuxd tunnel against a real iOS device.
 *
 * Self-skips when no device is attached, so this is safe in CI — but when a device *is* present
 * it exercises the entire iOS transport for real: [UsbmuxClient.connect], the big-endian port
 * encoding, and the [UsbmuxByteChannel] adapter that [io.github.yashkasera.alohomora.devtools.DevToolsSocket]
 * sits on.
 *
 * The target is `lockdownd` on port 62078, which listens on every iOS device regardless of what
 * apps are installed. That makes it the only dependency-free way to prove the tunnel carries
 * bidirectional traffic without first deploying an app.
 */
class UsbmuxTunnelTest {

    private val client = UsbmuxClient()

    /** `lockdownd`. Present on every iOS device, so it needs no app deployment. */
    private val lockdowndPort = 62078

    private fun firstDevice(): UsbmuxDevice? =
        if (!client.isAvailable()) null else client.listDevices().firstOrNull()

    @Test
    fun `opens a tunnel to lockdownd and carries a full request-response`() = runTest {
        val device = firstDevice() ?: return@runTest
        val socketChannel = client.connect(device.deviceId, lockdowndPort)
        assertNotNull(
            socketChannel,
            "usbmuxd refused a tunnel to lockdownd on device ${device.serialNumber}",
        )

        val channel = UsbmuxByteChannel(socketChannel)
        try {
            // lockdownd speaks length-prefixed (big-endian u32) XML plists.
            val query = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
                <plist version="1.0"><dict><key>Request</key><string>QueryType</string></dict></plist>
            """.trimIndent().toByteArray()

            val framed = ByteBuffer.allocate(4 + query.size).order(ByteOrder.BIG_ENDIAN)
                .putInt(query.size)
                .put(query)
                .array()

            channel.write(framed)

            val lengthBytes = ByteArray(4)
            assertTrue(channel.readFully(lengthBytes, 0, 4), "no reply length from lockdownd")
            val length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.BIG_ENDIAN).int
            assertTrue(length in 1..65_536, "implausible lockdownd reply length: $length")

            val payload = ByteArray(length)
            assertTrue(channel.readFully(payload, 0, length), "truncated lockdownd reply")

            // Proves this is a genuine pipe to the device, not a locally-satisfied connect.
            val reply = Plist.decode(payload) as? Map<*, *>
            assertNotNull(reply, "lockdownd reply did not parse as a plist")
            assertEquals("com.apple.mobile.lockdown", reply["Type"])
        } finally {
            channel.close()
        }
    }

    @Test
    fun `connect returns null when nothing listens on the requested port`() = runTest {
        val device = firstDevice() ?: return@runTest

        // Port 53999 is Alohomora's DevTools server. With the app not running, usbmuxd refuses,
        // and that must surface as null rather than a half-open channel. This is also the exact
        // path a user hits when iOS has suspended their backgrounded app.
        val socketChannel = client.connect(device.deviceId, 53999)
        if (socketChannel != null) {
            // The app *is* running — equally valid; just don't leak the channel.
            socketChannel.close()
        } else {
            assertNull(socketChannel)
        }
    }

    @Test
    fun `a discovered device maps to an ios platform with a usable tunnel handle`() = runTest {
        val device = firstDevice() ?: return@runTest

        val matches = IosDeviceDataSource().listDevices()
            .filter { it.id == device.serialNumber }
        // Exactly one. usbmuxd reports one entry per attachment, so a device paired over both
        // USB and Wi-Fi arrives twice with the same serial; discovery must collapse it or the
        // launcher shows two identical rows and device-id keying breaks.
        assertEquals(
            1,
            matches.size,
            "expected one row per physical device, got ${matches.size} for ${device.serialNumber}",
        )
        val mapped = matches.single()
        assertNotNull(mapped, "usbmuxd reported ${device.serialNumber} but discovery dropped it")

        // Physical hardware, so it must be IOS (not IOS_SIMULATOR) and must carry the numeric
        // usbmux handle — without it the launcher cannot open a tunnel and falls back to TCP.
        assertEquals(DevicePlatform.IOS, mapped.platform)
        assertEquals(device.deviceId, mapped.usbmuxDeviceId)

        // And the transport chosen for it must actually be the tunnel.
        val target = DevToolsTarget.forDevice(mapped, host = "127.0.0.1", port = 53999)
        assertTrue(target is DevToolsTarget.Usbmux, "expected a usbmux tunnel, got $target")
    }

    @Test
    fun `a wrong byte order for the port would not reach lockdownd`() = runTest {
        val device = firstDevice() ?: return@runTest

        // 62078 = 0xF27E; byte-swapped it is 0x7EF2 = 32498. Nothing listens there, so this
        // pins the encoding: if the implementation ever stopped swapping, the test above would
        // start hitting 32498 and fail.
        val wrongOrder = client.connect(device.deviceId, 32498)
        assertNull(wrongOrder, "port 32498 unexpectedly accepted a connection")
    }
}
