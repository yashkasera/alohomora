package io.github.yashkasera.alohomora.domain.usecase

import io.github.yashkasera.alohomora.sync.SyncService

internal class ConnectToRemoteUseCase(private val syncService: SyncService) {
    operator fun invoke(url: String) {
        syncService.connect(url)
    }
}
