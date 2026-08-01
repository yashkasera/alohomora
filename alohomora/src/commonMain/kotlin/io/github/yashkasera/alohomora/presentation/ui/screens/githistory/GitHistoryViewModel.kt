package io.github.yashkasera.alohomora.presentation.ui.screens.githistory

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.data.model.GitHistoryCommit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Immutable
internal data class GitHistoryState(
    val commits: List<GitHistoryCommit> = emptyList(),
    val isLoading: Boolean = false,
)

internal class GitHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow(GitHistoryState(isLoading = true))
    val state: StateFlow<GitHistoryState> = _state.asStateFlow()

    init {
        loadCommits()
    }

    private fun loadCommits() {
        try {
            val commits = Alohomora.config?.commits.orEmpty()
            _state.value = GitHistoryState(
                commits = commits,
                isLoading = false,
            )
        } catch (e: Exception) {
            _state.value = GitHistoryState(
                commits = emptyList(),
                isLoading = false,
            )
        }
    }
}
