package io.github.yashkasera.alohomora.desktop.domain.model

/** A `host:port` advertised by a device over mDNS for wireless debugging. */
data class WirelessEndpoint(val host: String, val port: Int) {
    val address: String get() = "$host:$port"
}

/**
 * Endpoints discovered via `adb mdns services`. Wireless debugging advertises two separate
 * services: one to pair against (with a code) and one to connect to afterwards.
 */
data class WirelessDiscovery(
    val pairing: WirelessEndpoint? = null,
    val connect: WirelessEndpoint? = null,
)
