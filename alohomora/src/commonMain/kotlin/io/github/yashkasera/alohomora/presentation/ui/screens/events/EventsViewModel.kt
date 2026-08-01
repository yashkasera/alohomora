package io.github.yashkasera.alohomora.presentation.ui.screens.events

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.data.paging.EventsPagingSource
import io.github.yashkasera.alohomora.domain.repository.EventsRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import io.github.yashkasera.alohomora.utils.paging.PagingData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class EventsState(
    val events: List<Event> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val showProperties: Boolean = true,
    val selectedEvent: Event? = null,
    val showSlackSheet: Boolean = false,
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
)

internal class EventsViewModel(
    private val telemetryRepository: EventsRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val showProperties = MutableStateFlow(true)
    private val selectedEvent = MutableStateFlow<Event?>(null)
    private val showSlackSheet = MutableStateFlow(false)
    private val showClearConfirmation = MutableStateFlow(false)
    private val isClearing = MutableStateFlow(false)
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { EventsPagingSource(telemetryRepository, searchQuery.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<EventsState> = combine(
        pager.pagingData,
        searchQuery,
        showProperties,
        selectedEvent,
        showSlackSheet,
        showClearConfirmation,
        isClearing,
    ) { flows: Array<*> ->
        val pagingData = flows[0] as PagingData<Event>
        val query = flows[1] as String
        val showProps = flows[2] as Boolean
        val selected = flows[3] as Event?
        val showSlack = flows[4] as Boolean
        val showClear = flows[5] as Boolean
        val clearing = flows[6] as Boolean
        EventsState(
            events = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
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

    init {
        loadNextItems()
    }

    fun loadNextItems() {
        pager.loadNextPage()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        pager.refresh()
    }

    fun toggleShowProperties() {
        showProperties.value = !showProperties.value
    }

    fun onEventClick(event: Event) {
        selectedEvent.value = event
        // Persisted, so the row stays dimmed across restarts — same contract as traces and
        // incidents. Guarded so an already-read event does not churn the DB on every reopen.
        if (!event.isViewed) {
            viewModelScope.launch { telemetryRepository.markAsViewed(event.id) }
        }
    }

    fun dismissEventDetail() {
        selectedEvent.value = null
    }

    fun showSlackSheet() {
        showSlackSheet.value = true
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
                telemetryRepository.clearAll()
                pager.refresh()
            } catch (_: Exception) {
                // Error handling could be added here
            } finally {
                isClearing.value = false
                showClearConfirmation.value = false
            }
        }
    }
}
