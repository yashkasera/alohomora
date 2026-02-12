package io.github.yashkasera.alohomora.desktop.domain.repository

import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot
import kotlinx.coroutines.flow.StateFlow

interface DatabaseRepository {
    val snapshot: StateFlow<DatabaseSnapshot>
    fun selectDatabase(name: String)
}
