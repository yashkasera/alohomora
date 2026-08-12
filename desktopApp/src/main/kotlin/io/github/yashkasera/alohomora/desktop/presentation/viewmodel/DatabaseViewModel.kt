package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.DatabaseRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseUpdateUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.DatabaseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DatabaseViewModel(
    private val repository: DatabaseRepository,
    private val requestDatabaseSchemaUseCase: RequestDatabaseSchemaUseCase,
    private val requestDatabaseTableUseCase: RequestDatabaseTableUseCase,
    private val requestDatabaseUpdateUseCase: RequestDatabaseUpdateUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val uiState: StateFlow<DatabaseUiState> = repository.snapshot
        .map { snapshot -> DatabaseUiState(snapshot) }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, DatabaseUiState(repository.snapshot.value))

    fun selectDatabase(name: String) {
        repository.selectDatabase(name)
        requestDatabaseSchemaUseCase(name)
    }

    fun requestTable(databaseName: String, tableName: String, limit: Int = 200) {
        requestDatabaseTableUseCase(databaseName, tableName, limit)
    }

    fun updateCell(
        databaseName: String,
        tableName: String,
        primaryKeys: Map<String, String>,
        columnName: String,
        newValue: String?,
    ) {
        requestDatabaseUpdateUseCase(databaseName, tableName, primaryKeys, columnName, newValue)
    }

    /**
     * Cancels this view model's scope.
     *
     * Required for per-window teardown: DesktopAppComposition.close() used to cancel
     * only DevToolsViewModel, so every other scope (and its collectors) leaked for the
     * life of the process each time a device window was closed.
     */
    fun close() {
        scope.cancel()
    }
}
