package io.github.yashkasera.alohomora.showcaseApp.domain.usecase

import io.github.yashkasera.alohomora.showcaseApp.domain.model.PreferencesState
import io.github.yashkasera.alohomora.showcaseApp.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class GetPreferencesUseCase(
    private val repository: PreferencesRepository,
) {
    operator fun invoke(): Flow<PreferencesState> = repository.observePreferences()
}
