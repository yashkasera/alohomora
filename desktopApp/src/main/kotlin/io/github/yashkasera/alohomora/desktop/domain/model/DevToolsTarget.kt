package io.github.yashkasera.alohomora.desktop.domain.model

/**
 * How to reach a device's DevTools server.
 *
 * Modelled explicitly because the protocol is identical across platforms but the transport is
 * not, and the difference is not expressible as "host + port":
 *
 *  - [Tcp] covers Android (via an `adb forward` on the host) and the iOS Simulator, which needs
 *    no tunnel at all because it runs on the host's own network stack.
 *  - [Usbmux] covers a physical iOS device, reached through macOS's usbmuxd over USB. There is
 *    no host-side port involved: usbmuxd returns a socket already wired to a port on the device,
 *    so there is nothing to allocate or tear down afterwards.
 */
sealed interface DevToolsTarget {

    /** Port on the far side of the transport. */
    val port: Int

    /** Label for the connection UI. */
    val displayHost: String

    data class Tcp(val host: String, override val port: Int) : DevToolsTarget {
        override val displayHost: String get() = host
    }

    data class Usbmux(val usbmuxDeviceId: Int, override val port: Int) : DevToolsTarget {
        override val displayHost: String get() = "usb"
    }

    companion object {
        /**
         * Chooses the transport for [device].
         *
         * @param host host to use for TCP targets (an `adb forward` endpoint, or the simulator's
         *   shared loopback).
         * @param port device-side port for a usbmux tunnel, or host-side port for TCP.
         */
        fun forDevice(device: Device, host: String, port: Int): DevToolsTarget = when {
            device.platform == DevicePlatform.IOS && device.usbmuxDeviceId != null ->
                Usbmux(device.usbmuxDeviceId, port)
            else -> Tcp(host, port)
        }
    }
}
