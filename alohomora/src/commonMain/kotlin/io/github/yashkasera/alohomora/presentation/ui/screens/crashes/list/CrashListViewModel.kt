package io.github.yashkasera.alohomora.presentation.ui.screens.crashes.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.Crash
import io.github.yashkasera.alohomora.domain.usecase.crash.ClearCrashesUseCase
import io.github.yashkasera.alohomora.domain.usecase.crash.GetCrashesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CrashListState(
    val crashes: List<Crash> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
)

internal class CrashListViewModel(
    private val getCrashesUseCase: GetCrashesUseCase,
    private val clearCrashesUseCase: ClearCrashesUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val state: StateFlow<CrashListState> = combine(
        _searchQuery,
        getCrashesUseCase()
    ) { query, crashes ->
        CrashListState(
            crashes = crashes,
            searchQuery = query,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CrashListState(isLoading = true),
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun clearAllCrashes() {
        viewModelScope.launch {
            clearCrashesUseCase()
        }
    }
}
