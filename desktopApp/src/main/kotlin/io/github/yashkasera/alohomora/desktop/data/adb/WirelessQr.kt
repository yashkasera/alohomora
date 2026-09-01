package io.github.yashkasera.alohomora.desktop.data.adb

/**
 * ADB Wireless-debugging QR pairing payload.
 *
 * This is the exact string Android's "Pair device with QR code" expects. After the phone scans
 * it, the phone advertises an mDNS `_adb-tls-pairing._tcp` service whose instance name equals
 * [serviceName]; the host finds that service (via `adb mdns services`) to learn the pairing
 * `ip:port`, then runs `adb pair ip:port <password>`.
 */
internal object WirelessQr {
    fun pairingPayload(serviceName: String, password: String): String =
        "WIFI:T:ADB;S:$serviceName;P:$password;;"
}
