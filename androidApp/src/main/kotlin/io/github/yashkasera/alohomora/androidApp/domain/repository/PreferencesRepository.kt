package io.github.yashkasera.alohomora.androidApp.domain.repository

import io.github.yashkasera.alohomora.androidApp.domain.model.PreferencesState
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    fun observePreferences(): Flow<PreferencesState>
    suspend fun updateUsername(value: String)
    suspend fun updateAutoRefresh(enabled: Boolean)
    suspend fun updateLastRefreshEpochMillis(value: Long)
}
