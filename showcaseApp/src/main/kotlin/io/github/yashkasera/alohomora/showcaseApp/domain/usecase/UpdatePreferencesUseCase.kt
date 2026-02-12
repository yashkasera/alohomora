package io.github.yashkasera.alohomora.showcaseApp.domain.usecase

import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PreferencesRepository

class UpdatePreferencesUseCase(
    private val repository: PreferencesRepository,
) {
    suspend fun updateUsername(value: String) {
        repository.updateUsername(value)
    }

    suspend fun updateAutoRefresh(enabled: Boolean) {
        repository.updateAutoRefresh(enabled)
    }
}
