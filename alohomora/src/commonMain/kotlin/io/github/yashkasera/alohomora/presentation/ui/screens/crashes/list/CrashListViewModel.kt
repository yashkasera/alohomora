package io.github.yashkasera.alohomora.presentation.ui.screens.crashes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Crash
import io.github.yashkasera.alohomora.data.paging.CrashPagingSource
import io.github.yashkasera.alohomora.domain.repository.CrashRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CrashListState(
    val crashes: List<Crash> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

internal class CrashListViewModel(
    private val crashRepository: CrashRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { CrashPagingSource(crashRepository, searchQuery.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<CrashListState> = combine(
        pager.pagingData,
        searchQuery,
    ) { pagingData, query ->
        CrashListState(
            crashes = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CrashListState(),
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

    fun clearAllCrashes() {
        viewModelScope.launch {
            crashRepository.clearAll()
        }
    }
}
