package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.CacheState
import io.github.yashkasera.alohomora.desktop.domain.repository.CacheRepository
import kotlinx.coroutines.flow.StateFlow

class CacheRepositoryImpl(
    store: CacheStore,
) : CacheRepository {
    override val state: StateFlow<CacheState> = store.state
}
