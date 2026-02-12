package io.github.yashkasera.alohomora.desktop.domain.usecase

import io.github.yashkasera.alohomora.desktop.domain.repository.DevToolsRepository

class RequestDatabaseTableUseCase(
    private val repository: DevToolsRepository,
) {
    operator fun invoke(databaseName: String, tableName: String, limit: Int = 200) {
        repository.requestDatabaseTable(databaseName, tableName, limit)
    }
}
