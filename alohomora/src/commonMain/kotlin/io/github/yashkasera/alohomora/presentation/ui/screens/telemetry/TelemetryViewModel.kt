package io.github.yashkasera.alohomora.presentation.ui.screens.telemetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.data.paging.TelemetryPagingSource
import io.github.yashkasera.alohomora.domain.repository.TelemetryRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TelemetryState(
    val events: List<TelemetryEvent> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

internal class TelemetryViewModel(
    telemetryRepository: TelemetryRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { TelemetryPagingSource(telemetryRepository, searchQuery.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<TelemetryState> = combine(
        pager.pagingData,
        searchQuery,
    ) { pagingData, query ->
        TelemetryState(
            events = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TelemetryState(),
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
}
