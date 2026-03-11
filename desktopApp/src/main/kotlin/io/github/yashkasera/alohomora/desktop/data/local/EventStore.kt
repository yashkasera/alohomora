package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TelemetryEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventStore {
    private val _events = MutableStateFlow<List<TelemetryEvent>>(emptyList())
    val events: StateFlow<List<TelemetryEvent>> = _events.asStateFlow()

    fun append(event: TelemetryEvent) {
        _events.value = (_events.value + event).takeLast(2000)
    }

    fun replace(events: List<TelemetryEvent>) {
        _events.value = events
    }

    fun clear() {
        _events.value = emptyList()
    }
}
