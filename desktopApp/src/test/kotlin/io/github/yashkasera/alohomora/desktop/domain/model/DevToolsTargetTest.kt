package io.github.yashkasera.alohomora.desktop.domain.model

import io.github.yashkasera.alohomora.desktop.presentation.ui.DesktopSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevToolsTargetTest {

    private fun device(
        platform: DevicePlatform,
        usbmuxDeviceId: Int? = null,
    ) = Device(
        id = "device",
        state = DeviceState.DEVICE,
        platform = platform,
        usbmuxDeviceId = usbmuxDeviceId,
    )

    @Test
    fun `physical ios devices use a usbmux tunnel, not a host port`() {
        val target = DevToolsTarget.forDevice(
            device(DevicePlatform.IOS, usbmuxDeviceId = 7),
            host = "127.0.0.1",
            port = 53999,
        )

        assertEquals(DevToolsTarget.Usbmux(usbmuxDeviceId = 7, port = 53999), target)
        // There is no adb-style host-side forward for iOS, so nothing host-shaped should leak
        // into the connection label.
        assertEquals("usb", target.displayHost)
    }

    @Test
    fun `ios simulators use plain tcp because they share the host network stack`() {
        val target = DevToolsTarget.forDevice(
            device(DevicePlatform.IOS_SIMULATOR),
            host = "127.0.0.1",
            port = 53999,
        )

        assertEquals(DevToolsTarget.Tcp("127.0.0.1", 53999), target)
    }

    @Test
    fun `android uses tcp through the adb forward`() {
        val target = DevToolsTarget.forDevice(
            device(DevicePlatform.ANDROID),
            host = "127.0.0.1",
            port = 53999,
        )

        assertEquals(DevToolsTarget.Tcp("127.0.0.1", 53999), target)
    }

    @Test
    fun `an ios device with no usbmux handle falls back rather than producing a bad tunnel`() {
        // Happens when the device is Wi-Fi paired but not plugged in: usbmuxd knows of it but we
        // have no handle. Must not fabricate a Usbmux target with a bogus id.
        val target = DevToolsTarget.forDevice(
            device(DevicePlatform.IOS, usbmuxDeviceId = null),
            host = "127.0.0.1",
            port = 53999,
        )

        assertTrue(target is DevToolsTarget.Tcp)
    }

    @Test
    fun `ios omits android-only sections and never defaults to the dashboard`() {
        val iosSections = DesktopSection.forPlatform(DevicePlatform.IOS)

        assertFalse(DesktopSection.Logcat in iosSections)
        assertFalse(DesktopSection.Adb in iosSections)
        assertFalse(DesktopSection.Dashboard in iosSections)

        // The protocol-backed sections are the whole point of connecting, so they must remain.
        assertTrue(DesktopSection.Traffic in iosSections)
        assertTrue(DesktopSection.Database in iosSections)
        assertTrue(DesktopSection.Cache in iosSections)
        assertTrue(DesktopSection.Events in iosSections)
        assertTrue(DesktopSection.GitHistory in iosSections)

        // Landing on Dashboard would open every iOS window on a permanently blank screen.
        assertEquals(DesktopSection.Traffic, DesktopSection.defaultFor(DevicePlatform.IOS))
    }

    @Test
    fun `android keeps every section and still defaults to the dashboard`() {
        assertEquals(
            DesktopSection.entries.toList(),
            DesktopSection.forPlatform(DevicePlatform.ANDROID),
        )
        assertEquals(DesktopSection.Dashboard, DesktopSection.defaultFor(DevicePlatform.ANDROID))
    }
}
