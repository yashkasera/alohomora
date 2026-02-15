package io.github.yashkasera.alohomora.presentation.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Analytics
import io.github.yashkasera.alohomora.domain.usecase.events.GetEventsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class EventsState(
    val events: List<Analytics> = emptyList(),
)

internal class EventsViewModel(
    getEventsUseCase: GetEventsUseCase,
) : ViewModel() {

    val state: StateFlow<EventsState> = getEventsUseCase()
        .map { events -> EventsState(events = events) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = EventsState(),
        )
}
