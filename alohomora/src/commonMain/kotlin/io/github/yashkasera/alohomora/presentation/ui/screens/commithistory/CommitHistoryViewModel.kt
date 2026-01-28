package io.github.yashkasera.alohomora.presentation.ui.screens.commithistory

import androidx.lifecycle.ViewModel
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.data.model.Commit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class CommitHistoryState(
    val commits: List<Commit> = emptyList(),
    val isLoading: Boolean = false,
)

internal class CommitHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(CommitHistoryState(isLoading = true))
    val state: StateFlow<CommitHistoryState> = _state.asStateFlow()

    init {
        loadCommits()
    }

    private fun loadCommits() {
        try {
            val commits = Alohomora.commits
            _state.value = CommitHistoryState(
                commits = commits,
                isLoading = false
            )
        } catch (e: Exception) {
            _state.value = CommitHistoryState(
                commits = emptyList(),
                isLoading = false
            )
        }
    }
}
