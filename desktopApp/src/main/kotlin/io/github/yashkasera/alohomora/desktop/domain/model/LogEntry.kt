package io.github.yashkasera.alohomora.desktop.domain.model

data class LogEntry(
    val timestamp: String,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val raw: String,
)
