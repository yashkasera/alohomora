package io.github.yashkasera.alohomora.presentation.ui.screens.trace.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.data.paging.TracePagingSource
import io.github.yashkasera.alohomora.domain.repository.TraceRepository
import io.github.yashkasera.alohomora.utils.paging.FlowPager
import io.github.yashkasera.alohomora.utils.paging.LoadState
import io.github.yashkasera.alohomora.utils.paging.PagingConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TraceState(
    val calls: List<TraceEntry> = emptyList(),
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val showClearConfirmation: Boolean = false,
    val isClearing: Boolean = false,
)

internal class TraceViewModel(
    private val traceRepository: TraceRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val method = MutableStateFlow("")
    private val showClearConfirmation = MutableStateFlow(false)
    private val isClearing = MutableStateFlow(false)
    private val pageSize = 20

    private val pager = FlowPager(
        config = PagingConfig(pageSize = pageSize),
        initialKey = 0,
        getNextKey = { it + 1 },
        pagingSourceFactory = { TracePagingSource(traceRepository, query.value, method.value) },
    ).cachedIn(viewModelScope)

    val state: StateFlow<TraceState> = combine(
        pager.pagingData,
        query,
        method,
        showClearConfirmation,
        isClearing,
    ) { pagingData, _, _, showClear, clearing ->
        TraceState(
            calls = pagingData.items,
            isLoadingMore = pagingData.loadState is LoadState.Loading,
            error = (pagingData.loadState as? LoadState.Error)?.error?.message,
            showClearConfirmation = showClear,
            isClearing = clearing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TraceState(),
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

    fun showClearConfirmation() {
        showClearConfirmation.value = true
    }

    fun hideClearConfirmation() {
        showClearConfirmation.value = false
    }

    fun clearAllTraces() {
        viewModelScope.launch {
            isClearing.value = true
            try {
                traceRepository.clearAll()
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
