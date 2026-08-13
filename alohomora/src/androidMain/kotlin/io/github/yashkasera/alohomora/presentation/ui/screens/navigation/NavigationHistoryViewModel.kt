package io.github.yashkasera.alohomora.presentation.ui.screens.navigation

import androidx.lifecycle.ViewModel
import io.github.yashkasera.alohomora.ActivityTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NavigationHistoryState(
    val timelineEvents: List<ActivityTimelineItem> = emptyList(),
    val sessionDuration: String = "00:00.00",
    val screensVisited: Int = 0,
)

internal class NavigationHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(NavigationHistoryState())
    val state: StateFlow<NavigationHistoryState> = _state.asStateFlow()

    init {
        loadNavigationHistory()
    }

    private fun loadNavigationHistory() {
        val events = ActivityTracker.events

        _state.value = NavigationHistoryState(
            timelineEvents = NavigationTimelineMapper.map(events, System.currentTimeMillis()),
            sessionDuration = NavigationTimelineMapper.sessionDuration(events),
            screensVisited = NavigationTimelineMapper.screensVisited(events),
        )
    }

    fun refresh() {
        loadNavigationHistory()
    }
}
