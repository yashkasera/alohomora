package io.github.yashkasera.alohomora.desktop.presentation.model

data class DashboardUiState(
    val androidVersion: String = "-",
    val apiLevel: String = "-",
    val batteryPercent: String = "-",
    val batteryStatus: String = "-",
    val memoryUsageGb: String = "-",
    val memoryTotalGb: String = "-",
    val cpuUsagePercent: String = "-",
    val networkMbPerSec: String = "-",
    val latencyMs: String = "-",
    val frameRateFps: String = "-",
    val frameTimeMs: String = "-",
    val jankFrames: String = "-",
    val actionMessage: String? = null,
    val actionError: String? = null,
    val loadingMetrics: Boolean = false,
)
