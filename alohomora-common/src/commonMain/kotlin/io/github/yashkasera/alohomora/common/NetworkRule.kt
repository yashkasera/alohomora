package io.github.yashkasera.alohomora.common

import kotlinx.serialization.Serializable

@Serializable
data class ThrottleProfile(
    val name: String,
    val latencyMs: Long = 0,
    val downloadBytesPerSec: Long = 0,
)

@Serializable
data class MockRule(
    val id: String,
    val enabled: Boolean = true,
    val urlPattern: String,
    val isRegex: Boolean = false,
    val method: String? = null,
    val statusCode: Int = 200,
    val responseBody: String = "",
    val contentType: String = "application/json",
)

object ThrottleProfiles {
    val NONE = ThrottleProfile("none")
    val EDGE = ThrottleProfile("edge", latencyMs = 800, downloadBytesPerSec = 6_000)
    val SLOW_3G = ThrottleProfile("3g", latencyMs = 400, downloadBytesPerSec = 40_000)
    val FAST_3G = ThrottleProfile("fast_3g", latencyMs = 150, downloadBytesPerSec = 200_000)
    val SLOW_WIFI = ThrottleProfile("slow_wifi", latencyMs = 50, downloadBytesPerSec = 500_000)
    val PRESETS = listOf(NONE, EDGE, SLOW_3G, FAST_3G, SLOW_WIFI)
}
