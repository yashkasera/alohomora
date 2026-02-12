package io.github.yashkasera.alohomora.desktop.data.logcat

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel

internal object LogcatParser {
    private val threadtimeRegex =
        Regex("""^(\d{2}-\d{2})\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s?(.*)$""")

    fun parseThreadtimeLine(line: String): LogEntry? {
        val match = threadtimeRegex.matchEntire(line.trim()) ?: return null
        val (date, time, pid, tid, level, tag, message) = match.destructured
        val parsedLevel = LogLevel.fromShortName(level) ?: return null

        return LogEntry(
            timestamp = "$date $time",
            pid = pid.toIntOrNull() ?: return null,
            tid = tid.toIntOrNull() ?: return null,
            level = parsedLevel,
            tag = tag.trim(),
            message = message.trim(),
            raw = line,
        )
    }
}
