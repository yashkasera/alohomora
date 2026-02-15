package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.ApiRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ApiLogStore {
    private val _logs = MutableStateFlow<List<ApiRequest>>(emptyList())
    val logs: StateFlow<List<ApiRequest>> = _logs.asStateFlow()

    fun append(log: ApiRequest) {
        _logs.value = (_logs.value + log).takeLast(2000)
    }

    fun replace(logs: List<ApiRequest>) {
        _logs.value = logs
    }

    fun clear() {
        _logs.value = emptyList()
    }
}
