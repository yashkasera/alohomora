package io.github.yashkasera.alohomora.showcaseApp.domain.repository

import io.github.yashkasera.alohomora.showcaseApp.domain.model.PreferencesState
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observePreferences(): Flow<PreferencesState>
    suspend fun updateUsername(value: String)
    suspend fun updateAutoRefresh(enabled: Boolean)
    suspend fun updateLastRefreshEpochMillis(value: Long)
}
