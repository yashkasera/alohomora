package io.github.yashkasera.alohomora.showcaseApp.domain.model

data class PreferencesState(
    val username: String,
    val autoRefresh: Boolean,
    val lastRefreshEpochMillis: Long,
)
