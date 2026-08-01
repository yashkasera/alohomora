package io.github.yashkasera.alohomora.desktop.data.ios

import io.github.yashkasera.alohomora.desktop.domain.model.Device
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Discovers iOS targets: physical devices over usbmuxd, and booted simulators over `simctl`.
 *
 * These are deliberately two different transports behind one list, because reaching them
 * differs fundamentally:
 *  - a physical device needs a usbmuxd tunnel (the `adb forward` equivalent);
 *  - a simulator needs nothing — it already shares the host's loopback.
 *
 * The prefix on simulator ids keeps the two id spaces from colliding: a usbmuxd serial and a
 * simulator UDID are both opaque strings, and the rest of the app keys sessions by device id.
 */
class IosDeviceDataSource(
    private val usbmux: UsbmuxClient = UsbmuxClient(),
    private val simctl: SimctlClient = SimctlClient(),
) {
    /** True on a host that can reach iOS targets at all (i.e. a Mac). */
    fun isSupported(): Boolean = usbmux.isAvailable() || simctl.isAvailable()

    suspend fun listDevices(): List<Device> = withContext(Dispatchers.IO) {
        // usbmuxd reports one entry per *attachment*, not per device: a phone paired over both
        // USB and Wi-Fi comes back twice, with two different DeviceIDs but the same
        // SerialNumber. Since Device.id is the serial, that surfaced as two identical-looking
        // rows in the launcher — and would have collided in list keys and session lookups.
        //
        // Collapse per serial, preferring the USB attachment: it is lower latency and does not
        // depend on the phone staying on the same network.
        val physical = usbmux.listDevices()
            .groupBy { it.serialNumber }
            .map { (serial, attachments) ->
                val preferred = attachments.firstOrNull { it.isUsb } ?: attachments.first()
                Device(
                    id = serial,
                    state = DeviceState.DEVICE,
                    // No device name is available from usbmuxd, so label by how we reach it.
                    // Without this the UI falls back to printing the serial twice.
                    model = if (preferred.isUsb) "iPhone (USB)" else "iPhone (Wi-Fi)",
                    product = null,
                    transportId = preferred.deviceId.toString(),
                    platform = DevicePlatform.IOS,
                    usbmuxDeviceId = preferred.deviceId,
                )
            }

        val simulators = simctl.listBootedSimulators().map { simulator ->
            Device(
                id = SIMULATOR_ID_PREFIX + simulator.udid,
                state = DeviceState.DEVICE,
                model = simulator.name,
                product = simulator.dataPath,
                transportId = null,
                platform = DevicePlatform.IOS_SIMULATOR,
                usbmuxDeviceId = null,
            )
        }

        physical + simulators
    }

    companion object {
        const val SIMULATOR_ID_PREFIX = "sim:"

        /** Recovers the raw UDID from an id produced by [listDevices]. */
        fun simulatorUdid(deviceId: String): String? =
            deviceId.removePrefix(SIMULATOR_ID_PREFIX).takeIf { it != deviceId }
    }
}
