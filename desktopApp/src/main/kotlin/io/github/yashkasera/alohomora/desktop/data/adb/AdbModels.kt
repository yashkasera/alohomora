package io.github.yashkasera.alohomora.desktop.data.adb

import kotlinx.serialization.Serializable

@Serializable
enum class AdbDeviceState {
    DEVICE,
    OFFLINE,
    UNAUTHORIZED,
    UNKNOWN,
}

data class AdbDevice(
    val id: String,
    val state: AdbDeviceState,
    val model: String? = null,
    val product: String? = null,
    val transportId: String? = null,
)

data class AdbCommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)
