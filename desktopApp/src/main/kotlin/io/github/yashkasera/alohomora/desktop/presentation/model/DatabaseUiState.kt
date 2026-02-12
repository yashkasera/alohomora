package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.desktop.domain.model.DatabaseSnapshot

data class DatabaseUiState(
    val snapshot: DatabaseSnapshot,
)
