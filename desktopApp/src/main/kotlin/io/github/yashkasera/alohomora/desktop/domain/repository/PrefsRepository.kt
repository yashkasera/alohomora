package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import kotlinx.coroutines.flow.StateFlow

interface PrefsRepository {
    val state: StateFlow<PrefsState>
}
