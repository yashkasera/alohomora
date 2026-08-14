package io.github.yashkasera.alohomora.presentation.ui.screens.database

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import io.github.yashkasera.alohomora.domain.repository.DatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
internal data class DatabaseState(
    val databases: List<DatabaseInfo> = emptyList(),
    val selectedDatabase: DatabaseInfo? = null,
    val tables: List<String> = emptyList(),
    val selectedTable: String? = null,
    val currentTab: Int = 1, // 0: Browse, 1: Query, 2: Schema
    val queryText: String = "",
    val queryResults: TableData? = null,
    val queryStatus: QueryStatus? = null,
    val tableData: TableData? = null,
    val tableSchema: TableSchema? = null,
    val showDatabaseSelector: Boolean = false,
    val isLoadingDatabases: Boolean = false,
    val isLoadingTables: Boolean = false,
    val isLoadingTableData: Boolean = false,
    val isLoadingSchema: Boolean = false,
    val isExecutingQuery: Boolean = false,
    val error: String? = null,
)

internal data class QueryStatus(
    val success: Boolean,
    val executionTimeMs: Long,
    val rowsAffected: Int = 0,
)

internal class DatabaseViewModel(
    private val vaultRepository: DatabaseRepository,
) : ViewModel() {

    private val databases = MutableStateFlow<List<DatabaseInfo>>(emptyList())
    private val selectedDatabase = MutableStateFlow<DatabaseInfo?>(null)
    private val tables = MutableStateFlow<List<String>>(emptyList())
    private val selectedTable = MutableStateFlow<String?>(null)
    private val currentTab = MutableStateFlow(1)
    private val queryText = MutableStateFlow("")
    private val queryResults = MutableStateFlow<TableData?>(null)
    private val queryStatus = MutableStateFlow<QueryStatus?>(null)
    private val tableData = MutableStateFlow<TableData?>(null)
    private val tableSchema = MutableStateFlow<TableSchema?>(null)
    private val showDatabaseSelector = MutableStateFlow(false)
    private val isLoadingDatabases = MutableStateFlow(false)
    private val isLoadingTables = MutableStateFlow(false)
    private val isLoadingTableData = MutableStateFlow(false)
    private val isLoadingSchema = MutableStateFlow(false)
    private val isExecutingQuery = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<DatabaseState> = combine(
        databases,
        selectedDatabase,
        tables,
        selectedTable,
        currentTab,
        queryText,
        queryResults,
        queryStatus,
        tableData,
        tableSchema,
        showDatabaseSelector,
        isLoadingDatabases,
        isLoadingTables,
        isLoadingTableData,
        isLoadingSchema,
        isExecutingQuery,
        error,
    ) { flows ->
        DatabaseState(
            databases = flows[0] as List<DatabaseInfo>,
            selectedDatabase = flows[1] as DatabaseInfo?,
            tables = flows[2] as List<String>,
            selectedTable = flows[3] as String?,
            currentTab = flows[4] as Int,
            queryText = flows[5] as String,
            queryResults = flows[6] as TableData?,
            queryStatus = flows[7] as QueryStatus?,
            tableData = flows[8] as TableData?,
            tableSchema = flows[9] as TableSchema?,
            showDatabaseSelector = flows[10] as Boolean,
            isLoadingDatabases = flows[11] as Boolean,
            isLoadingTables = flows[12] as Boolean,
            isLoadingTableData = flows[13] as Boolean,
            isLoadingSchema = flows[14] as Boolean,
            isExecutingQuery = flows[15] as Boolean,
            error = flows[16] as String?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DatabaseState(),
    )

    init {
        loadDatabases()
    }

    private fun loadDatabases() {
        viewModelScope.launch {
            isLoadingDatabases.value = true
            error.value = null
            try {
                vaultRepository.listDatabases().collect { dbList ->
                    databases.value = dbList
                    // Auto-select first database if available and none selected
                    if (selectedDatabase.value == null && dbList.isNotEmpty()) {
                        selectDatabase(dbList.first())
                    }
                }
            } catch (e: Exception) {
                error.value = "Failed to load databases: ${e.message}"
            } finally {
                isLoadingDatabases.value = false
            }
        }
    }

    fun selectDatabase(database: DatabaseInfo) {
        if (selectedDatabase.value?.path == database.path) {
            showDatabaseSelector.value = false
            return
        }

        selectedDatabase.value = database
        selectedTable.value = null
        tableData.value = null
        tableSchema.value = null
        queryResults.value = null
        queryStatus.value = null
        showDatabaseSelector.value = false

        loadTables(database.path)
    }

    private fun loadTables(databasePath: String) {
        viewModelScope.launch {
            isLoadingTables.value = true
            error.value = null
            try {
                vaultRepository.listTables(databasePath).collect { tableList ->
                    tables.value = tableList
                    // Auto-select first table if available
                    if (tableList.isNotEmpty()) {
                        selectTable(tableList.first())
                    }
                }
            } catch (e: Exception) {
                error.value = "Failed to load tables: ${e.message}"
            } finally {
                isLoadingTables.value = false
            }
        }
    }

    fun selectTable(table: String) {
        if (selectedTable.value == table) return

        selectedTable.value = table
        tableData.value = null
        tableSchema.value = null

        selectedDatabase.value?.let { db ->
            loadTableData(db.path, table)
            loadTableSchema(db.path, table)
        }
    }

    private fun loadTableData(databasePath: String, tableName: String) {
        viewModelScope.launch {
            isLoadingTableData.value = true
            error.value = null
            try {
                vaultRepository.getTableData(databasePath, tableName).collect { data ->
                    tableData.value = data
                }
            } catch (e: Exception) {
                error.value = "Failed to load table data: ${e.message}"
            } finally {
                isLoadingTableData.value = false
            }
        }
    }

    private fun loadTableSchema(databasePath: String, tableName: String) {
        viewModelScope.launch {
            isLoadingSchema.value = true
            error.value = null
            try {
                vaultRepository.getTableSchema(databasePath, tableName).collect { schema ->
                    tableSchema.value = schema
                }
            } catch (e: Exception) {
                error.value = "Failed to load table schema: ${e.message}"
            } finally {
                isLoadingSchema.value = false
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        currentTab.value = tabIndex
    }

    fun updateQueryText(text: String) {
        queryText.value = text
    }

    fun executeQuery() {
        val dbPath = selectedDatabase.value?.path ?: return
        val query = queryText.value.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            isExecutingQuery.value = true
            error.value = null
            try {
                vaultRepository.executeQuery(dbPath, query).collect { result ->
                    queryResults.value = result.data
                    queryStatus.value = QueryStatus(
                        success = result.success,
                        executionTimeMs = result.executionTimeMs,
                        rowsAffected = result.rowsAffected,
                    )
                    if (!result.success) {
                        error.value = result.errorMessage
                    }
                }
            } catch (e: Exception) {
                error.value = "Query execution failed: ${e.message}"
                queryStatus.value = QueryStatus(
                    success = false,
                    executionTimeMs = 0,
                    rowsAffected = 0,
                )
            } finally {
                isExecutingQuery.value = false
            }
        }
    }

    fun toggleDatabaseSelector(show: Boolean) {
        showDatabaseSelector.value = show
    }

    fun refresh() {
        loadDatabases()
    }
}
