package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestDatabaseSchemaUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(databaseName: String) {
        repository.requestDatabaseSchema(databaseName)
    }
}
