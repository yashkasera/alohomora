package io.github.yashkasera.alohomora.desktop.data.adb

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
