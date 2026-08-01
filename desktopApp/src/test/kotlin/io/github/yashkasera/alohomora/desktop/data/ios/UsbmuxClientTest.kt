package io.github.yashkasera.alohomora.desktop.data.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UsbmuxClientTest {

    @Test
    fun `reports unavailable when there is no usbmuxd socket`() {
        val client = UsbmuxClient(socketPath = "/tmp/definitely-not-usbmuxd-${System.nanoTime()}")

        assertFalse(client.isAvailable())
        // Must degrade, not throw: on Linux/Windows there is no usbmuxd at all.
        assertEquals(emptyList(), client.listDevices())
        assertNull(client.connect(deviceId = 1, port = 53999))
    }

    @Test
    fun `talks to the real usbmuxd socket when present`() {
        val client = UsbmuxClient()
        if (!client.isAvailable()) return // not a Mac, or the service is absent

        // An empty list is a valid answer (nothing plugged in). What matters is that the
        // handshake completes and the reply parses, rather than throwing or hanging.
        val devices = client.listDevices()
        devices.forEach { device ->
            assertTrue(device.deviceId > 0, "usbmuxd device id must be positive: $device")
            assertTrue(device.serialNumber.isNotBlank(), "serial must be present: $device")
        }
    }

    @Test
    fun `connect fails cleanly for a device id that does not exist`() {
        val client = UsbmuxClient()
        if (!client.isAvailable()) return

        // No device has this handle, so usbmuxd must refuse and we must return null rather
        // than hand back a half-open channel.
        assertNull(client.connect(deviceId = Int.MAX_VALUE, port = 53999))
    }
}
