package io.github.yashkasera.alohomora.androidApp.presentation

import androidx.lifecycle.ViewModel
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.androidApp.domain.usecase.GetPreferencesUseCase
import io.github.yashkasera.alohomora.androidApp.domain.usecase.ObservePostsUseCase
import io.github.yashkasera.alohomora.androidApp.domain.usecase.RefreshPostsUseCase
import io.github.yashkasera.alohomora.androidApp.domain.usecase.UpdatePreferencesUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PostsViewModel(
    observePostsUseCase: ObservePostsUseCase,
    getPreferencesUseCase: GetPreferencesUseCase,
    private val refreshPostsUseCase: RefreshPostsUseCase,
    private val updatePreferencesUseCase: UpdatePreferencesUseCase,
) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val loadingState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)

    val uiState = combine(
        observePostsUseCase(),
        getPreferencesUseCase(),
        loadingState,
        errorState
    ) { posts, preferences, isLoading, error ->
        UiState(
            posts = posts,
            preferences = preferences,
            isLoading = isLoading,
            errorMessage = error
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), UiState())

    init {
        refreshPosts()
    }

    fun refreshPosts() {
        scope.launch {
            loadingState.value = true
            errorState.value = null
            try {
                refreshPostsUseCase()
            } catch (e: Exception) {
                errorState.value = e.message ?: "Failed to refresh posts"
            } finally {
                loadingState.value = false
            }
        }
    }

    fun onPostClicked(postId: Long) {
        Alohomora.trackEvent("post_clicked", mapOf("postId" to postId.toString()))
    }

    fun updateUsername(value: String) {
        scope.launch {
            updatePreferencesUseCase.updateUsername(value)
        }
    }

    fun updateAutoRefresh(enabled: Boolean) {
        scope.launch {
            updatePreferencesUseCase.updateAutoRefresh(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        scope.cancel()
    }
}
