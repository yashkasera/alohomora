package io.github.yashkasera.alohomora.desktop.presentation.viewmodel

import io.github.yashkasera.alohomora.desktop.domain.repository.DatabaseRepository
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseSchemaUseCase
import io.github.yashkasera.alohomora.desktop.domain.usecase.RequestDatabaseTableUseCase
import io.github.yashkasera.alohomora.desktop.presentation.model.DatabaseUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class DatabaseViewModel(
    private val repository: DatabaseRepository,
    private val requestDatabaseSchemaUseCase: RequestDatabaseSchemaUseCase,
    private val requestDatabaseTableUseCase: RequestDatabaseTableUseCase,
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
}
