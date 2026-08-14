package io.github.yashkasera.alohomora.traffic

import io.github.yashkasera.alohomora.common.TrafficEntry

internal fun interface TrafficNotificationCallback {
    suspend fun onTrafficUpdated(latest: List<TrafficEntry>)
}
