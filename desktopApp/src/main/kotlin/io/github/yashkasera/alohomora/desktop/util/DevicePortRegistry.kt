package io.github.yashkasera.alohomora.desktop.util

class DevicePortRegistry(
    private val basePort: Int = 53999,
) {
    private val portsByDevice = mutableMapOf<String, Int>()

    fun getPort(deviceId: String): Int? = portsByDevice[deviceId]

    fun assignPort(deviceId: String): Int {
        portsByDevice[deviceId]?.let { return it }
        val used = portsByDevice.values.toSet()
        var candidate = basePort
        while (used.contains(candidate)) {
            candidate += 1
        }
        portsByDevice[deviceId] = candidate
        return candidate
    }

    fun setPort(deviceId: String, port: Int) {
        portsByDevice[deviceId] = port
    }

    fun snapshot(): Map<String, Int> = portsByDevice.toMap()

    fun clear(deviceId: String) {
        portsByDevice.remove(deviceId)
    }
}
