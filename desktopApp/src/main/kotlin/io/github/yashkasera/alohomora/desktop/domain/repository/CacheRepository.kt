package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
import kotlinx.coroutines.flow.StateFlow

interface CacheRepository {
    val state: StateFlow<CacheState>
}
