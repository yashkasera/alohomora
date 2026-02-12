package io.github.yashkasera.alohomora.showcaseApp.presentation

import io.github.yashkasera.alohomora.showcaseApp.domain.model.Post
import io.github.yashkasera.alohomora.showcaseApp.domain.model.PreferencesState

data class UiState(
    val posts: List<Post> = emptyList(),
    val preferences: PreferencesState = PreferencesState(
        username = "",
        autoRefresh = false,
        lastRefreshEpochMillis = 0L
    ),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
