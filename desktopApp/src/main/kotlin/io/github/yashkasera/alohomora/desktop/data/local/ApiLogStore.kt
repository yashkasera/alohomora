package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TraceEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiLogStore {
    private val _logs = MutableStateFlow<List<TraceEntry>>(emptyList())
    val logs: StateFlow<List<TraceEntry>> = _logs.asStateFlow()

    fun append(log: TraceEntry) {
        _logs.value = (_logs.value + log).takeLast(2000)
    }

    fun replace(logs: List<TraceEntry>) {
        _logs.value = logs
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
