package io.github.yashkasera.alohomora.presentation.ui.screens.events

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.utils.paging.LiveWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

@Immutable
internal data class EventsState(
    /** The loaded window, newest first. Grows as the list scrolls; see [EventsViewModel.loadMore]. */
    val events: List<Event> = emptyList(),
    /** Every event matching the search, loaded or not — what the subtitle reports. */
    val totalCount: Long = 0,
    val searchQuery: String = "",
    val showProperties: Boolean = true,
    val selectedEvent: Event? = null,
    val showSlackSheet: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
)

internal class EventsViewModel(
    private val eventsRepository: EventsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val showProperties = MutableStateFlow(true)
    private val selectedEvent = MutableStateFlow<Event?>(null)
    private val showSlackSheet = MutableStateFlow(false)
    private val showClearConfirmation = MutableStateFlow(false)
    private val isClearing = MutableStateFlow(false)

    private val window = LiveWindow("", PAGE_SIZE) { query, limit ->
        eventsRepository.list(query = query, pageSize = limit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val totalCount = searchQuery.flatMapLatest { eventsRepository.count(it) }

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<EventsState> = combine(
        window.items,
        totalCount,
        searchQuery,
        showProperties,
        selectedEvent,
        showSlackSheet,
        showClearConfirmation,
        isClearing,
    ) { flows: Array<*> ->
        val events = flows[0] as List<Event>
        val total = flows[1] as Long
        val query = flows[2] as String
        val showProps = flows[3] as Boolean
        val selected = flows[4] as Event?
        val showSlack = flows[5] as Boolean
        val showClear = flows[6] as Boolean
        val clearing = flows[7] as Boolean
        EventsState(
            events = events,
            totalCount = total,
            searchQuery = query,
            showProperties = showProps,
            selectedEvent = selected,
            showSlackSheet = showSlack,
            showClearConfirmation = showClear,
            isClearing = clearing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsState(),
    )

    /** Called as the list nears its end. Safe to call repeatedly — see [LiveWindow.loadMore]. */
    fun loadMore() {
        window.loadMore()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        window.setFilter(query)
    }

    fun toggleShowProperties() {
        showProperties.value = !showProperties.value
    }

    fun onEventClick(event: Event) {
        selectedEvent.value = event
        // Persisted, so the row stays dimmed across restarts — same contract as traces and
        // incidents. Guarded so an already-read event does not churn the DB on every reopen.
        if (!event.isViewed) {
            viewModelScope.launch { eventsRepository.markAsViewed(event.id) }
        }
    }

    fun dismissEventDetail() {
        selectedEvent.value = null
    }

    fun hideSlackSheet() {
        showSlackSheet.value = false
    }

    fun showClearConfirmation() {
        showClearConfirmation.value = true
    }

    fun hideClearConfirmation() {
        showClearConfirmation.value = false
    }

    fun clearAllEvents() {
        viewModelScope.launch {
            isClearing.value = true
            try {
                // No refresh: the window is a live query, and Room re-emits it on the delete.
                eventsRepository.clearAll()
            } catch (_: Exception) {
            } finally {
                isClearing.value = false
                showClearConfirmation.value = false
            }
        }
    }
}
