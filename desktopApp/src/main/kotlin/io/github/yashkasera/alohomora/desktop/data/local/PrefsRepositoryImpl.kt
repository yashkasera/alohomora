package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.desktop.domain.model.PrefsState
import io.github.yashkasera.alohomora.desktop.domain.repository.PrefsRepository
import kotlinx.coroutines.flow.StateFlow

class PrefsRepositoryImpl(
    private val store: PrefsStore,
) : PrefsRepository {
    override val state: StateFlow<PrefsState> = store.state
}
