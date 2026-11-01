package io.github.yashkasera.alohomora.presentation.ui.screens.error.list

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.domain.repository.ErrorRepository
import io.github.yashkasera.alohomora.utils.paging.LiveWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

@Immutable
internal data class ErrorState(
    /** The loaded window, newest first. Grows as the list scrolls; see [ErrorViewModel.loadMore]. */
    val errors: List<Error> = emptyList(),
    /** Every error matching the search, loaded or not — what the subtitle reports. */
    val totalCount: Long = 0,
    val searchQuery: String = "",
)

internal class ErrorViewModel(
    private val errorRepository: ErrorRepository,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    private val window = LiveWindow("", PAGE_SIZE) { query, limit ->
        errorRepository.list(query = query, pageSize = limit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val totalCount = searchQuery.flatMapLatest { errorRepository.count(it) }

    val state: StateFlow<ErrorState> = combine(
        window.items,
        totalCount,
        searchQuery,
    ) { errors, total, query ->
        ErrorState(
            errors = errors,
            totalCount = total,
            searchQuery = query,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ErrorState(),
    )

    /** Called as the list nears its end. Safe to call repeatedly — see [LiveWindow.loadMore]. */
    fun loadMore() {
        window.loadMore()
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        window.setFilter(query)
    }

    fun clearAllErrors() {
        viewModelScope.launch {
            errorRepository.clearAll()
        }
    }
}
