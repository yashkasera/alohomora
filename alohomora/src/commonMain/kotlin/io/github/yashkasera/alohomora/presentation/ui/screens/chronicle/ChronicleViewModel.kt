package io.github.yashkasera.alohomora.presentation.ui.screens.chronicle

import androidx.lifecycle.ViewModel
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.data.model.AlohomoraCommit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ChronicleState(
    val commits: List<AlohomoraCommit> = emptyList(),
    val isLoading: Boolean = false,
)

internal class ChronicleViewModel : ViewModel() {

    private val _state = MutableStateFlow(ChronicleState(isLoading = true))
    val state: StateFlow<ChronicleState> = _state.asStateFlow()

    init {
        loadCommits()
    }

    private fun loadCommits() {
        try {
            val commits = Alohomora.config?.commits.orEmpty()
            _state.value = ChronicleState(
                commits = commits,
                isLoading = false,
            )
        } catch (e: Exception) {
            _state.value = ChronicleState(
                commits = emptyList(),
                isLoading = false,
            )
        }
    }
}
