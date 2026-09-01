package io.github.yashkasera.alohomora.desktop.data.adb

/** One entry from `adb mdns services` — a wireless-debugging endpoint advertised on the LAN. */
internal data class MdnsService(
    val name: String,
    val type: String,
    val host: String,
    val port: Int,
) {
    val isPairing: Boolean get() = type.contains("_adb-tls-pairing")
    val isConnect: Boolean get() = type.contains("_adb-tls-connect")
}

internal object AdbParser {
    fun parseDevices(output: String): List<AdbDevice> {
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("List of devices attached") }
            .mapNotNull { parseDeviceLine(it) }
            .toList()
    }

    /**
     * Parses `adb mdns services` output. Lines look like:
     * `adb-SERIAL-xxxx	_adb-tls-connect._tcp.	192.168.1.5:42159`
     * (columns whitespace/tab separated; the service type may carry a trailing dot).
     */
    fun parseMdnsServices(output: String): List<MdnsService> {
        return output
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.startsWith("List of discovered mdns services") }
            .mapNotNull { parseMdnsLine(it) }
            .toList()
    }

    private fun parseMdnsLine(line: String): MdnsService? {
        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 3) return null
        val address = parts[2]
        val separator = address.lastIndexOf(':')
        if (separator <= 0) return null
        val host = address.substring(0, separator)
        val port = address.substring(separator + 1).toIntOrNull() ?: return null
        return MdnsService(name = parts[0], type = parts[1].trimEnd('.'), host = host, port = port)
    }

    private fun parseDeviceLine(line: String): AdbDevice? {
        val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.size < 2) return null
        val id = parts[0]
        val stateToken = parts[1]
        val state = when (stateToken) {
            "device" -> AdbDeviceState.DEVICE
            "offline" -> AdbDeviceState.OFFLINE
            "unauthorized" -> AdbDeviceState.UNAUTHORIZED
            else -> AdbDeviceState.UNKNOWN
        }

        var model: String? = null
        var product: String? = null
        var transportId: String? = null

        parts.drop(2).forEach { token ->
            when {
                token.startsWith("model:") -> model = token.substringAfter("model:")
                token.startsWith("product:") -> product = token.substringAfter("product:")
                token.startsWith("transport_id:") -> transportId =
                    token.substringAfter("transport_id:")
            }
        }

        return AdbDevice(
            id = id,
            state = state,
            model = model?.ifBlank { null },
            product = product?.ifBlank { null },
            transportId = transportId?.ifBlank { null },
        )
    }
}
