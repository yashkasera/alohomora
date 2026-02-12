package io.github.yashkasera.alohomora.desktop.presentation.ui

enum class DesktopSection(
    val title: String,
    val subtitle: String,
    val railLabel: String,
) {
    Devices("Devices", "CONNECTED TARGETS", "DEV"),
    Logcat("Logcat", "LIVE LOG STREAM", "LOG"),
    Events("Events", "SYSTEM TRIGGERS", "EVT"),
    ApiLogs("API Logs", "LIVE STREAM", "API"),
    Database("Database", "Inspector", "DB"),
    Preferences("Preferences", "Memory Store", "PREF"),
}
