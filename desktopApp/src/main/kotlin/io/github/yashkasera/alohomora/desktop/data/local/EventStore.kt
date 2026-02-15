package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.Analytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventStore {
    private val _events = MutableStateFlow<List<Analytics>>(emptyList())
    val events: StateFlow<List<Analytics>> = _events.asStateFlow()

    fun append(event: Analytics) {
        _events.value = (_events.value + event).takeLast(2000)
    }

    fun replace(events: List<Analytics>) {
        _events.value = events
    }

    fun clear() {
        _events.value = emptyList()
    }
}
