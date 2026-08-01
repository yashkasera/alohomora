package io.github.yashkasera.alohomora.desktop.domain.model

data class Device(
    val id: String,
    val state: DeviceState,
    val model: String? = null,
    val product: String? = null,
    val transportId: String? = null,
    val platform: DevicePlatform = DevicePlatform.ANDROID,
    /**
     * usbmuxd's numeric device handle, required to open a tunnel. Only set for
     * [DevicePlatform.IOS]; usbmuxd identifies devices by this int, not by serial.
     */
    val usbmuxDeviceId: Int? = null,
) {
    val capabilities: Set<DeviceCapability> get() = DeviceCapability.forPlatform(platform)

    fun supports(capability: DeviceCapability): Boolean = capability in capabilities
}
