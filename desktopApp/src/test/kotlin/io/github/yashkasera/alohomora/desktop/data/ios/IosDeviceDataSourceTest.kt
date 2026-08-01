package io.github.yashkasera.alohomora.desktop.data.ios

import io.github.yashkasera.alohomora.desktop.domain.model.DeviceCapability
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class IosDeviceDataSourceTest {

    @Test
    fun `simulator ids are namespaced so they cannot collide with device serials`() {
        val udid = "B91F4ABD-DD10-4B23-B927-FA5F78329682"
        val namespaced = IosDeviceDataSource.SIMULATOR_ID_PREFIX + udid

        assertEquals(udid, IosDeviceDataSource.simulatorUdid(namespaced))
        // A physical device serial must not be mistaken for a simulator.
        assertNull(IosDeviceDataSource.simulatorUdid("00008140-000E4DA60AC0801C"))
    }

    @Test
    fun `ios devices get ios capabilities, not android ones`() {
        val ios = DeviceCapability.forPlatform(DevicePlatform.IOS)

        // These are all dumpsys/logcat/screenrecord-derived and have no iOS equivalent, so the
        // UI must not offer them for an iOS device.
        assertFalse(DeviceCapability.OS_LOG_STREAM in ios)
        assertFalse(DeviceCapability.DEVICE_METRICS in ios)
        assertFalse(DeviceCapability.SCREEN_RECORD in ios)
        assertFalse(DeviceCapability.CONNECTIVITY_TOGGLES in ios)
        assertFalse(DeviceCapability.SHELL in ios)

        assertTrue(DeviceCapability.APP_INSTALL in ios)
    }

    @Test
    fun `android keeps every capability`() {
        assertEquals(
            DeviceCapability.entries.toSet(),
            DeviceCapability.forPlatform(DevicePlatform.ANDROID),
        )
    }

    @Test
    fun `platform labels distinguish hardware from simulator`() {
        assertEquals("Android", DevicePlatform.ANDROID.label)
        assertEquals("iOS", DevicePlatform.IOS.label)
        assertEquals("iOS Simulator", DevicePlatform.IOS_SIMULATOR.label)
        assertTrue(DevicePlatform.IOS.isIos && DevicePlatform.IOS_SIMULATOR.isIos)
        assertFalse(DevicePlatform.ANDROID.isIos)
    }

    @Test
    fun `degrades to an empty list when no ios tooling is present`() = runTest {
        val source = IosDeviceDataSource(
            usbmux = UsbmuxClient(socketPath = "/tmp/no-usbmuxd-${System.nanoTime()}"),
            simctl = SimctlClient(xcrunPath = "/tmp/no-xcrun-${System.nanoTime()}"),
        )

        assertFalse(source.isSupported())
        // Must not throw: the desktop app also runs on Linux and Windows, where no iOS
        // tooling exists at all.
        assertEquals(emptyList(), source.listDevices())
    }
}
