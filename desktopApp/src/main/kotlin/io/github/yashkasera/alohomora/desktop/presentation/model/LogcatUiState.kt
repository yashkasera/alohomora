package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel

data class LogcatFilterState(
    val enabledLevels: Set<LogLevel> = LogLevel.values().toSet(),
    val selectedTag: String? = null,
    val searchQuery: String = "",
)

data class LogcatUiState(
    val entries: List<LogEntry>,
    val filteredEntries: List<LogEntry>,
    val availableTags: List<String>,
    val filterState: LogcatFilterState,
    val running: Boolean,
    val errorMessage: String?,
    val selectedDeviceId: String?,
)
