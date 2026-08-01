package io.github.yashkasera.alohomora.desktop.data.ios

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for duplicate device rows in the launcher.
 *
 * usbmuxd returns one entry per *attachment*, not per device. A phone paired over both USB and
 * Wi-Fi is reported twice — two different `DeviceID`s, the same `SerialNumber`. Because
 * `Device.id` is the serial, that produced two identical-looking rows in the device list, and
 * would also have broken anything keyed by device id (list keys, per-window sessions, the
 * `forwards` map).
 *
 * This tests the collapsing rule directly, since building it into the data source would
 * otherwise require a live device paired two ways to reproduce.
 */
class UsbmuxDeduplicationTest {

    private val serial = "00008140-000E4DA60AC0801C"

    /** Mirrors the grouping in [IosDeviceDataSource.listDevices]. */
    private fun collapse(attachments: List<UsbmuxDevice>): List<UsbmuxDevice> =
        attachments.groupBy { it.serialNumber }
            .map { (_, group) -> group.firstOrNull { it.isUsb } ?: group.first() }

    @Test
    fun `one device paired twice collapses to a single entry`() {
        val collapsed = collapse(
            listOf(
                UsbmuxDevice(deviceId = 2016, serialNumber = serial, connectionType = "Network"),
                UsbmuxDevice(deviceId = 2017, serialNumber = serial, connectionType = "USB"),
            ),
        )

        assertEquals(1, collapsed.size, "expected one row for one physical device")
    }

    @Test
    fun `usb attachment wins over network`() {
        // USB is lower latency and does not depend on the phone staying on this network, so it
        // must be preferred regardless of the order usbmuxd happens to return.
        val networkFirst = collapse(
            listOf(
                UsbmuxDevice(2016, serial, "Network"),
                UsbmuxDevice(2017, serial, "USB"),
            ),
        ).single()
        assertEquals(2017, networkFirst.deviceId)

        val usbFirst = collapse(
            listOf(
                UsbmuxDevice(2017, serial, "USB"),
                UsbmuxDevice(2016, serial, "Network"),
            ),
        ).single()
        assertEquals(2017, usbFirst.deviceId)
    }

    @Test
    fun `a wifi-only device is still listed`() {
        // The common case while a cable is charge-only: no USB attachment exists, and dropping
        // the device entirely would make it undebuggable.
        val collapsed = collapse(listOf(UsbmuxDevice(2016, serial, "Network"))).single()

        assertEquals(2016, collapsed.deviceId)
        assertTrue(!collapsed.isUsb)
    }

    @Test
    fun `distinct devices are not merged`() {
        val collapsed = collapse(
            listOf(
                UsbmuxDevice(2016, serial, "USB"),
                UsbmuxDevice(2018, "00008150-001961121A0A401C", "USB"),
            ),
        )

        assertEquals(2, collapsed.size)
        assertEquals(
            setOf(serial, "00008150-001961121A0A401C"),
            collapsed.map { it.serialNumber }.toSet(),
        )
    }

    @Test
    fun `connection type matching is case insensitive`() {
        // usbmuxd has reported both "USB" and "Usb" across OS versions.
        assertTrue(UsbmuxDevice(1, serial, "usb").isUsb)
        assertTrue(UsbmuxDevice(1, serial, "USB").isUsb)
        assertTrue(!UsbmuxDevice(1, serial, "Network").isUsb)
    }
}
