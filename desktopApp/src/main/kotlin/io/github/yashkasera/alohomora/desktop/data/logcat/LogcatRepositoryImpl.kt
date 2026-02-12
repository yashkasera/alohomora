package io.github.yashkasera.alohomora.desktop.data.logcat

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.repository.LogcatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

class LogcatRepositoryImpl(
    private val maxEntries: Int = 5000,
    private val streamDataSource: LogcatStreamDataSource = LogcatStreamDataSource(),
) : LogcatRepository {
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    override val entries: StateFlow<List<LogEntry>> = _entries

    override fun streamEntries(deviceId: String): Flow<LogEntry> {
        return streamDataSource.streamThreadtime(deviceId)
            .mapNotNull { line -> LogcatParser.parseThreadtimeLine(line) }
    }

    override fun append(entry: LogEntry) {
        _entries.update { current ->
            (current + entry).takeLast(maxEntries)
        }
    }

    override fun clear() {
        _entries.value = emptyList()
    }
}
