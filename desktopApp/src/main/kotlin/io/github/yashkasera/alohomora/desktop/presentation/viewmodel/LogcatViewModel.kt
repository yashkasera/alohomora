package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel
import io.github.yashkasera.alohomora.desktop.domain.repository.LogcatRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.ClearLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.ObserveLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StartLogcatUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.StopLogcatUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.LogcatFilterState
import io.github.yashkasera.alohomora.desktop.presentation.model.LogcatUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LogcatViewModel(
    private val repository: LogcatRepository,
    private val observeLogcatUseCase: ObserveLogcatUseCase,
    private val startLogcatUseCase: StartLogcatUseCase,
    private val stopLogcatUseCase: StopLogcatUseCase,
    private val clearLogcatUseCase: ClearLogcatUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var logcatJob: Job? = null

    private val _filterState = MutableStateFlow(LogcatFilterState())
    private val _running = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectedDeviceId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LogcatUiState> = combine(
        observeLogcatUseCase(),
        _filterState,
        _running,
        _error,
        _selectedDeviceId,
    ) { entries, filter, running, error, deviceId ->
        val availableTags = entries.map { it.tag }.distinct().sorted()
        val filtered = applyFilters(entries, filter)
        LogcatUiState(
            entries = entries,
            filteredEntries = filtered,
            availableTags = availableTags,
            filterState = filter,
            running = running,
            errorMessage = error,
            selectedDeviceId = deviceId,
        )
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        LogcatUiState(
            entries = emptyList(),
            filteredEntries = emptyList(),
            availableTags = emptyList(),
            filterState = LogcatFilterState(),
            running = false,
            errorMessage = null,
            selectedDeviceId = null,
        )
    )

    fun setSelectedDevice(deviceId: String?) {
        _selectedDeviceId.value = deviceId
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun updateTagFilter(tag: String) {
        _filterState.value = _filterState.value.copy(tagFilter = tag)
    }

    fun toggleRegex() {
        _filterState.value = _filterState.value.copy(isRegex = !_filterState.value.isRegex)
    }

    fun updatePackageName(packageName: String) {
        _filterState.value = _filterState.value.copy(packageName = packageName)
    }

    fun toggleLevel(level: LogLevel) {
        val current = _filterState.value.enabledLevels
        val updated = if (current.contains(level)) current - level else current + level
        _filterState.value = _filterState.value.copy(enabledLevels = updated)
    }

    fun start() {
        val deviceId = _selectedDeviceId.value
        if (deviceId.isNullOrBlank()) {
            _error.value = "Select a device to start logcat"
            return
        }
        stop()
        _error.value = null
        _running.value = true
        logcatJob = scope.launch {
            try {
                startLogcatUseCase(deviceId).collect { entry ->
                    repository.append(entry)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to start logcat"
            } finally {
                _running.value = false
            }
        }
    }

    fun stop() {
        logcatJob?.cancel()
        logcatJob = null
        _running.value = false
        stopLogcatUseCase()
    }

    fun clear() {
        _error.value = null
        clearLogcatUseCase()
    }

    private fun applyFilters(entries: List<LogEntry>, filter: LogcatFilterState): List<LogEntry> {
        if (entries.isEmpty()) return emptyList()
        val packageQuery = filter.packageName.trim().lowercase()

        val tagTokens = filter.tagFilter.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        val tagIncludes = tagTokens.filter { !it.startsWith("-") }.map { it.lowercase() }
        val tagExcludes = tagTokens.filter { it.startsWith("-") }
            .map { it.removePrefix("-").trim().lowercase() }
            .filter { it.isNotEmpty() }

        val searchRegex = if (filter.isRegex && filter.searchQuery.isNotBlank()) {
            try { Regex(filter.searchQuery.trim(), RegexOption.IGNORE_CASE) } catch (_: Exception) { null }
        } else null

        val searchTokens = if (!filter.isRegex) {
            filter.searchQuery.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()
        val searchIncludes = searchTokens.filter { !it.startsWith("-") }.map { it.lowercase() }
        val searchExcludes = searchTokens.filter { it.startsWith("-") }
            .map { it.removePrefix("-").trim().lowercase() }
            .filter { it.isNotEmpty() }

        return entries.asSequence()
            .filter { filter.enabledLevels.contains(it.level) }
            .filter { entry ->
                val tag = entry.tag.lowercase()
                val includePass = tagIncludes.isEmpty() || tagIncludes.any { tag.contains(it) }
                val excludePass = tagExcludes.none { tag.contains(it) }
                includePass && excludePass
            }
            .filter { entry ->
                if (packageQuery.isEmpty()) return@filter true
                val haystack = "${entry.tag} ${entry.message} ${entry.raw}".lowercase()
                haystack.contains(packageQuery)
            }
            .filter { entry ->
                if (filter.isRegex) {
                    if (searchRegex == null) return@filter true
                    val haystack = "${entry.tag} ${entry.message}"
                    searchRegex.containsMatchIn(haystack)
                } else {
                    if (searchIncludes.isEmpty() && searchExcludes.isEmpty()) return@filter true
                    val haystack = "${entry.tag} ${entry.message} ${entry.pid} ${entry.tid}".lowercase()
                    val includePass = searchIncludes.isEmpty() || searchIncludes.any { haystack.contains(it) }
                    val excludePass = searchExcludes.none { haystack.contains(it) }
                    includePass && excludePass
                }
            }
            .toList()
    }

    /**
     * Cancels this view model's scope.
     *
     * Required for per-window teardown: DesktopAppComposition.close() used to cancel
     * only DevToolsViewModel, so every other scope (and its collectors) leaked for the
     * life of the process each time a device window was closed.
     */
    fun close() {
        scope.cancel()
    }
}
