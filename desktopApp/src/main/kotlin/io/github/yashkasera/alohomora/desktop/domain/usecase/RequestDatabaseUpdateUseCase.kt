package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestDatabaseUpdateUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(
        databaseName: String,
        tableName: String,
        primaryKeys: Map<String, String>,
        columnName: String,
        newValue: String?,
    ) {
        repository.requestDatabaseUpdate(databaseName, tableName, primaryKeys, columnName, newValue)
    }
}
