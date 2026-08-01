package io.github.yashkasera.alohomora.presentation.ui.screens.error.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.data.paging.ErrorPagingSource
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
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
internal data class ErrorState(
    val errors: List<Error> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)

internal class ErrorViewModel(
    private val errorRepository: ErrorRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { ErrorPagingSource(errorRepository, searchQuery.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<ErrorState> = combine(
        pager.pagingData,
        searchQuery,
    ) { pagingData, query ->
        ErrorState(
            errors = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ErrorState(),
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

    fun clearAllErrors() {
        viewModelScope.launch {
            errorRepository.clearAll()
        }
    }
}
