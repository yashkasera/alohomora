package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.DeviceCapability
import io.github.yashkasera.alohomora.desktop.domain.model.DevicePlatform
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState

data class DeviceUi(
    val id: String,
    val state: DeviceState,
    val model: String? = null,
    val platform: DevicePlatform = DevicePlatform.ANDROID,
    val capabilities: Set<DeviceCapability> = DeviceCapability.forPlatform(platform),
    /** usbmuxd's numeric handle; required to open a tunnel to a physical iOS device. */
    val usbmuxDeviceId: Int? = null,
)
