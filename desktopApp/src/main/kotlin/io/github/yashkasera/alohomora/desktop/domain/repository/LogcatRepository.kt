package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface LogcatRepository {
    val entries: StateFlow<List<LogEntry>>

    fun streamEntries(deviceId: String): Flow<LogEntry>
    fun append(entry: LogEntry)
    fun clear()
}
