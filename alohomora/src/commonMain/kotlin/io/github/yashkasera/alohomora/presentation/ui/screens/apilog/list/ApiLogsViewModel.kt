package io.github.yashkasera.alohomora.presentation.ui.screens.apilog.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.ApiRequest
import io.github.yashkasera.alohomora.data.paging.NetworkPagingSource
import io.github.yashkasera.alohomora.domain.repository.NetworkRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ApiLogsState(
    val calls: List<ApiRequest> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)

internal class ApiLogsViewModel(
    networkRepository: NetworkRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val method = MutableStateFlow("")
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { NetworkPagingSource(networkRepository, query.value, method.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<ApiLogsState> = combine(
        pager.pagingData,
        query,
        method,
    ) { pagingData, _, _ ->
        ApiLogsState(
            calls = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ApiLogsState(),
    )

    init {
        loadNextItems()
    }

    fun loadNextItems() {
        pager.loadNextPage()
    }

    fun setQuery(newQuery: String?) {
        query.value = newQuery ?: ""
        pager.refresh()
    }

    fun setMethod(newMethod: String?) {
        method.value = newMethod ?: ""
        pager.refresh()
    }
}
