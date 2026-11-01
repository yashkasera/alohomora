package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import io.github.yashkasera.alohomora.utils.paging.LiveWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class TrafficState(
    /** The loaded window, newest first. Grows as the list scrolls; see [TrafficViewModel.loadMore]. */
    val calls: List<TrafficEntry> = emptyList(),
    /** Every entry matching the filters, loaded or not — what the subtitle reports. */
    val totalCount: Long = 0,
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
)

private data class TrafficFilter(val query: String = "", val method: String = "")

private const val PAGE_SIZE = 20

internal class TrafficViewModel(
    private val trafficRepository: TrafficRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _method = MutableStateFlow("")
    val method = _method.asStateFlow()
    private val showClearConfirmation = MutableStateFlow(false)
    private val isClearing = MutableStateFlow(false)

    private val filter = MutableStateFlow(TrafficFilter())

    private val window = LiveWindow(TrafficFilter(), PAGE_SIZE) { filter, limit ->
        trafficRepository.list(query = filter.query, method = filter.method, pageSize = limit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val totalCount = filter.flatMapLatest { trafficRepository.count(it.query, it.method) }

    val state: StateFlow<TrafficState> = combine(
        window.items,
        totalCount,
        showClearConfirmation,
        isClearing,
    ) { calls, total, showClear, clearing ->
        TrafficState(
            calls = calls,
            totalCount = total,
            showClearConfirmation = showClear,
            isClearing = clearing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrafficState(),
    )

    /** Called as the list nears its end. Safe to call repeatedly — see [LiveWindow.loadMore]. */
    fun loadMore() {
        window.loadMore()
    }

    fun setQuery(newQuery: String?) {
        _query.value = newQuery ?: ""
        applyFilter()
    }

    fun setMethod(newMethod: String?) {
        _method.value = newMethod ?: ""
        applyFilter()
    }

    private fun applyFilter() {
        val next = TrafficFilter(query = _query.value, method = _method.value)
        filter.value = next
        window.setFilter(next)
    }

    fun showClearConfirmation() {
        showClearConfirmation.value = true
    }

    fun hideClearConfirmation() {
        showClearConfirmation.value = false
    }

    fun clearAllTraffic() {
        viewModelScope.launch {
            isClearing.value = true
            try {
                // No refresh: the window is a live query, and Room re-emits it on the delete.
                trafficRepository.clearAll()
            } catch (_: Exception) {
            } finally {
                isClearing.value = false
                showClearConfirmation.value = false
            }
        }
    }
}
