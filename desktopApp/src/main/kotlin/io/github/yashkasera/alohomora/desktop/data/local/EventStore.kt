package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventStore {
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    fun append(event: Event) {
        _events.value = (_events.value + event).takeLast(2000)
    }

    fun replace(events: List<Event>) {
        _events.value = events
    }

    fun clear() {
        _events.value = emptyList()
    }
}
