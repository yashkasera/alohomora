package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.ApiLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiLogStore {
    private val _logs = MutableStateFlow<List<ApiLog>>(emptyList())
    val logs: StateFlow<List<ApiLog>> = _logs.asStateFlow()

    fun append(log: ApiLog) {
        _logs.value = (_logs.value + log).takeLast(2000)
    }

    fun replace(logs: List<ApiLog>) {
        _logs.value = logs
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
