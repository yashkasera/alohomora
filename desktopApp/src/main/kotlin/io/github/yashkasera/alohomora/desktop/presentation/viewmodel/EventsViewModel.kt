package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.desktop.data.devtools.DesktopEventPrefs
import io.github.yashkasera.alohomora.desktop.data.local.EventStore
import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository
import io.github.yashkasera.alohomora.desktop.presentation.model.EventSearchIndex
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsFilterState
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsPredicate
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsTimeWindow
import io.github.yashkasera.alohomora.desktop.presentation.model.EventsUiState
import io.github.yashkasera.alohomora.desktop.presentation.model.IndexedEvent
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the Events panel's filter state and derived list.
 *
 * Separate from [DevToolsViewModel] for the reason [TracesViewModel] documents: `DevToolsViewModel` is
 * handed to every panel in the window, so filtering and search-indexing up to [EventStore.MAX_ENTRIES]
 * events there would stay warm whether or not anyone is looking at Events. `WhileSubscribed` with a
 * grace period keeps it warm across a quick tab switch instead.
 *
 * It also fixes an incidental annoyance: `showProperties` used to be `remember` state in the panel, so
 * the toggle reset every time the user switched panels and came back.
 */
class EventsViewModel(
    private val repository: DevToolsRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val index = EventSearchIndex()

    private val _filters = MutableStateFlow(EventsFilterState())

    private val _showProperties = MutableStateFlow(true)
    val showProperties: StateFlow<Boolean> = _showProperties.asStateFlow()

    private val _selectedEventId = MutableStateFlow<Long?>(null)
    val selectedEventId: StateFlow<Long?> = _selectedEventId.asStateFlow()

    /**
     * Every held event paired with the text a query runs against.
     *
     * A `StateFlow` rather than a bare `map`: [EventSearchIndex] holds a mutable cache and must be
     * driven by exactly one collector, and two `stateIn` downstreams of a cold flow would each run the
     * lambda. Sharing here is also what makes the haystacks cost one build per *store change* rather
     * than one per keystroke.
     */
    private val indexed: StateFlow<List<IndexedEvent>> = repository.events
        .map { events -> index.reindex(events) }
        .stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), emptyList())

    /**
     * The rolling window's floor, moved once a second.
     *
     * A ticker is required, not decorative: the floor advances even when the stream is idle, so without
     * one a "last 30s" list keeps showing events that aged out the moment the device went quiet.
     * `flatMapLatest` on the window means no ticker exists at all while the filter is
     * [EventsTimeWindow.All], and `WhileSubscribed` on [uiState] means none runs while the panel is off
     * screen.
     *
     * A tick that changes nothing costs nothing downstream: `stateIn` conflates on equality and both
     * [EventsUiState] and `List<Event>` compare structurally, so an unchanged second does not
     * recompose the list.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val rollingFloor: Flow<Long?> = _filters
        .map { it.window }
        .distinctUntilChanged()
        .flatMapLatest { window ->
            if (window == EventsTimeWindow.All) {
                flowOf(null)
            } else {
                flow {
                    while (true) {
                        emit(window.floorAt(Clock.System.now().toEpochMilliseconds()))
                        delay(TICK_MILLIS)
                    }
                }
            }
        }

    val uiState: StateFlow<EventsUiState> =
        combine(indexed, _filters, rollingFloor) { events, filters, floor ->
            // The stricter of the two. Both mean "hide older than X", so Mark and a rolling window
            // intersect rather than override each other.
            val effectiveFloor = listOfNotNull(floor, filters.markFloorMillis).maxOrNull()
            val predicate = EventsPredicate(filters, effectiveFloor)
            // Counted over everything held and before the mute set is applied; see EventsUiState.
            val counts = events.groupingBy { it.event.name }.eachCount()
            EventsUiState(
                events = events.filter(predicate::matches).map(IndexedEvent::event),
                totalCount = events.size,
                nameCounts = counts,
                names = counts.entries
                    .sortedWith(
                        compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
                    )
                    .map { it.key },
                atStoreCap = events.size >= EventStore.MAX_ENTRIES,
                filters = filters,
            )
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), EventsUiState())

    /**
     * The open event, re-resolved from the store on every change.
     *
     * Holds an id rather than the [Event], unlike the traffic sheet's `selectedTrafficForSheet`, which
     * has a latent staleness bug: [DevToolsRepository.markEventViewed] replaces the instance in the
     * store, so a captured copy renders an `isViewed` that disagrees with the row behind the sheet.
     * Re-resolving also drops the selection when the event disappears on a clear or a device switch —
     * the same trick [TraceDetailState.selectedSpanId] uses.
     */
    val selectedEvent: StateFlow<Event?> =
        combine(repository.events, _selectedEventId) { events, id ->
            id?.let { selected -> events.firstOrNull { it.id == selected } }
        }.stateIn(scope, SharingStarted.WhileSubscribed(SUBSCRIPTION_GRACE_MILLIS), null)

    init {
        scope.launch {
            // Reloaded per device, not once. `filterNotNull` because the id starts null and a load under
            // a null key would wipe a mute made before the id arrived.
            repository.currentDeviceId.filterNotNull().collect { deviceId ->
                // The transient filters are dropped along with the reload, not just the mutes: a Mark
                // floor pinned against the previous device's clock can sit above everything the new one
                // sends, which reads as a dead stream. Every store was just cleared anyway.
                _filters.value =
                    EventsFilterState(mutedNames = DesktopEventPrefs.mutedNames(deviceId))
                _selectedEventId.value = null
            }
        }
    }

    fun onQueryChange(query: String) = _filters.update { it.copy(query = query) }

    fun onUnreadOnlyChange(unreadOnly: Boolean) =
        _filters.update { it.copy(unreadOnly = unreadOnly) }

    fun onWindowChange(window: EventsTimeWindow) = _filters.update { it.copy(window = window) }

    /**
     * Pins a floor just above the newest event currently held.
     *
     * Deliberately not `Clock.System.now()`. Event times are stamped on the device, and a device clock
     * ahead of this host would make "hide everything before now" also hide the next few seconds of
     * arrivals. Anchoring to the newest held event makes the mark mean what the user pointed at. The
     * rolling window cannot use the same anchor, because an idle stream would freeze it and the window
     * would never empty — so that one keeps the wall clock and inherits the skew, which is surfaced
     * rather than corrected.
     *
     * Reads [DevToolsRepository.events] rather than [indexed]. The latter is `WhileSubscribed`, so its
     * value is empty whenever nothing is collecting, and this would then fall back to the wall clock —
     * the very skew it exists to avoid. Only the wholly empty store reaches that fallback now.
     */
    fun mark() {
        val anchor = repository.events.value.firstOrNull()?.time
            ?: Clock.System.now().toEpochMilliseconds()
        _filters.update { it.copy(markFloorMillis = anchor + 1) }
    }

    fun clearMark() = _filters.update { it.copy(markFloorMillis = null) }

    fun toggleMute(name: String) = updateMuted { it.withMuteToggled(name) }

    /**
     * "Show only this", which as an exclusion means muting every other name currently held.
     *
     * Reads the repository rather than [indexed] for the reason [mark] documents: a `WhileSubscribed`
     * value can be empty, and here that would silently solo against nothing and mute no one.
     */
    fun soloName(name: String) =
        updateMuted { it.withSolo(name, repository.events.value.map { event -> event.name }) }

    fun unmuteAll() = updateMuted { it.withMutesCleared() }

    /**
     * Drops the session filters and keeps the mutes, which are persistent and deliberate — the
     * "N muted" control is their own way out.
     */
    fun clearFilters() = _filters.update { it.withTransientCleared() }

    fun openEvent(id: Long) {
        _selectedEventId.value = id
        repository.markEventViewed(id)
    }

    fun closeEvent() {
        _selectedEventId.value = null
    }

    fun toggleShowProperties() {
        _showProperties.value = !_showProperties.value
    }

    fun clearEvents() {
        closeEvent()
        repository.clearCaptured(events = true)
    }

    fun close() {
        scope.cancel()
    }

    /**
     * Applies a pure mute transformation and persists the result.
     *
     * The transformations themselves live on [EventsFilterState] so they are testable without a
     * repository; this wrapper exists only to pair every mute change with its write to disk, so no
     * caller can change the set and forget to persist it.
     */
    private fun updateMuted(transform: (EventsFilterState) -> EventsFilterState) {
        val next = transform(_filters.value)
        _filters.value = next
        DesktopEventPrefs.saveMutedNames(repository.currentDeviceId.value, next.mutedNames)
    }

    private companion object {
        /** Keeps the index and the filter warm across a quick panel switch; see [TracesViewModel]. */
        const val SUBSCRIPTION_GRACE_MILLIS = 5_000L

        /** One second. Finer buys nothing against a floor the chip label reads to the second. */
        const val TICK_MILLIS = 1_000L
    }
}
