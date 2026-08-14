package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.data.paging.TrafficPagingSource
import io.github.yashkasera.alohomora.domain.repository.TrafficRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class TrafficState(
    val calls: List<TrafficEntry> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
)

internal class TrafficViewModel(
    private val traceRepository: TrafficRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _method = MutableStateFlow("")
    val method = _method.asStateFlow()
    private val showClearConfirmation = MutableStateFlow(false)
    private val isClearing = MutableStateFlow(false)
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = {
            TrafficPagingSource(traceRepository, query.value, method.value)
        },
    ).cachedIn(viewModelScope)

    val state: StateFlow<TrafficState> = combine(
        pager.pagingData,
        query,
        method,
        showClearConfirmation,
        isClearing,
    ) { pagingData, _, _, showClear, clearing ->
        TrafficState(
            calls = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
            showClearConfirmation = showClear,
            isClearing = clearing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrafficState(),
    )

    init {
        loadNextItems()
    }

    fun loadNextItems() {
        pager.loadNextPage()
    }

    fun setQuery(newQuery: String?) {
        _query.value = newQuery ?: ""
        pager.refresh()
    }

    fun setMethod(newMethod: String?) {
        _method.value = newMethod ?: ""
        pager.refresh()
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
                traceRepository.clearAll()
                pager.refresh()
            } catch (_: Exception) {
            } finally {
                isClearing.value = false
                showClearConfirmation.value = false
            }
        }
    }
}
