package io.github.yashkasera.alohomora.showcaseApp.data.repository

import io.github.yashkasera.alohomora.showcaseApp.data.preferences.PreferencesDataSource
import io.github.yashkasera.alohomora.showcaseApp.domain.model.PreferencesState
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepositoryImpl(
    private val dataSource: PreferencesDataSource,
) : PreferencesRepository {

    private val stateFlow = MutableStateFlow(readState())

    override fun observePreferences(): Flow<PreferencesState> = stateFlow.asStateFlow()

    override suspend fun updateUsername(value: String) {
        dataSource.writeUsername(value)
        stateFlow.value = stateFlow.value.copy(username = value)
    }

    override suspend fun updateAutoRefresh(enabled: Boolean) {
        dataSource.writeAutoRefresh(enabled)
        stateFlow.value = stateFlow.value.copy(autoRefresh = enabled)
    }

    override suspend fun updateLastRefreshEpochMillis(value: Long) {
        dataSource.writeLastRefreshEpochMillis(value)
        stateFlow.value = stateFlow.value.copy(lastRefreshEpochMillis = value)
    }

    private fun readState(): PreferencesState {
        return PreferencesState(
            username = dataSource.readUsername(),
            autoRefresh = dataSource.readAutoRefresh(),
            lastRefreshEpochMillis = dataSource.readLastRefreshEpochMillis()
        )
    }
}
