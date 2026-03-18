package io.github.yashkasera.alohomora.presentation.ui.screens.incident.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Incident
import io.github.yashkasera.alohomora.data.paging.IncidentPagingSource
import io.github.yashkasera.alohomora.domain.repository.IncidentRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class IncidentState(
    val incidents: List<Incident> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

internal class IncidentViewModel(
    private val incidentRepository: IncidentRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { IncidentPagingSource(incidentRepository, searchQuery.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<IncidentState> = combine(
        pager.pagingData,
        searchQuery,
    ) { pagingData, query ->
        IncidentState(
            incidents = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = IncidentState(),
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

    fun clearAllIncidents() {
        viewModelScope.launch {
            incidentRepository.clearAll()
        }
    }
}
