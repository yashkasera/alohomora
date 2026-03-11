package io.github.yashkasera.alohomora.presentation.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.presentation.ui.components.icons.ArrowLeft
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Settings
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextFieldDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun VaultScreen(
    onBackClick: () -> Unit,
    viewModel: VaultViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Vault",
                subtitle = null,
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    AlohomoraIconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Database Selector
            DatabaseSelector(
                selectedDatabase = state.selectedDatabase,
                onClick = { viewModel.toggleDatabaseSelector(true) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tables Section
            TablesSection(
                tables = state.tables,
                selectedTable = state.selectedTable,
                onTableSelected = { viewModel.selectTable(it) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tabs and Content
            TabsWithContent(
                currentTab = state.currentTab,
                onTabSelected = { viewModel.selectTab(it) },
                queryText = state.queryText,
                onQueryTextChanged = { viewModel.updateQueryText(it) },
                onRunQuery = { viewModel.executeQuery() },
                queryResults = state.queryResults,
                queryStatus = state.queryStatus,
                tableData = state.tableData,
                tableSchema = state.tableSchema
            )
        }

        // Bottom Sheet for Database Selection
        if (state.showDatabaseSelector) {
            DatabaseSelectorBottomSheet(
                databases = state.databases,
                selectedDatabase = state.selectedDatabase,
                onDatabaseSelected = { viewModel.selectDatabase(it) },
                onDismiss = { viewModel.toggleDatabaseSelector(false) }
            )
        }
    }
}

@Composable
fun DatabaseSelector(
    selectedDatabase: DatabaseInfo?,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = "ACTIVE DATABASE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(0.dp)
                )
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDatabase?.name ?: "Select Database",
                    style = MaterialTheme.typography.headlineMedium,
                    fontStyle = FontStyle.Italic
                )
                Text(
                    text = "▼",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
fun TablesSection(
    tables: List<String>,
    selectedTable: String?,
    onTableSelected: (String) -> Unit
) {
    Column {
        Text(
            text = "TABLES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            tables.forEach { table ->
                Text(
                    text = table,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (table == selectedTable) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.clickable { onTableSelected(table) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        AlohomoraHorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
    }
}

@Composable
fun TabsWithContent(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    queryText: String,
    onQueryTextChanged: (String) -> Unit,
    onRunQuery: () -> Unit,
    queryResults: TableData?,
    queryStatus: QueryStatus?,
    tableData: TableData?,
    tableSchema: TableSchema?
) {
    val tabs = listOf("BROWSE", "QUERY", "SCHEMA")
    val pagerState = rememberPagerState(initialPage = currentTab, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(currentTab) {
        pagerState.animateScrollToPage(currentTab)
    }

    LaunchedEffect(pagerState.currentPage) {
        onTabSelected(pagerState.currentPage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    }
                ) {
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (pagerState.currentPage == index) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.onBackground)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> BrowseTabContent(tableData = tableData)
                1 -> QueryTabContent(
                    queryText = queryText,
                    onQueryTextChanged = onQueryTextChanged,
                    onRunQuery = onRunQuery,
                    queryResults = queryResults,
                    queryStatus = queryStatus
                )
                2 -> SchemaTabContent(tableSchema = tableSchema)
            }
        }
    }
}

@Composable
fun BrowseTabContent(tableData: TableData?) {
    if (tableData != null) {
        DataTableViewer(tableData = tableData)
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No data available", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun QueryTabContent(
    queryText: String,
    onQueryTextChanged: (String) -> Unit,
    onRunQuery: () -> Unit,
    queryResults: TableData?,
    queryStatus: QueryStatus?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Query Input
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(16.dp)
        ) {
            Column {
                AlohomoraTextField(
                    value = queryText,
                    onValueChange = onQueryTextChanged,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    colors = AlohomoraTextFieldDefaults.textFieldColors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SQLite 3.39.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    AlohomoraFilledButton(
                        onClick = onRunQuery,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        shape = RoundedCornerShape(4.dp),
                        text = "Run",
                    ) {
                        Text("▶  RUN", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Query Results
        Column(modifier = Modifier.weight(0.6f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Query Results",
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic
                )

                queryStatus?.let { status ->
                    Surface(
                        color = if (status.success) {
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        } else {
                            Color(0xFFF44336).copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (status.success) "Success" else "Error",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (status.success) {
                                    Color(0xFF4CAF50)
                                } else {
                                    Color(0xFFF44336)
                                }
                            )
                            Text(
                                text = "${status.executionTimeMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (queryResults != null) {
                DataTableViewer(tableData = queryResults)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Run a query to see results", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun SchemaTabContent(tableSchema: TableSchema?) {
    if (tableSchema != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Table Name
            Text(
                text = "Table: ${tableSchema.name}",
                style = MaterialTheme.typography.headlineSmall,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Columns
            Text(
                text = "COLUMNS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(12.dp))

            tableSchema.columns.forEach { column ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = column.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = column.type,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Key
            if (tableSchema.primaryKey != null) {
                Text(
                    text = "PRIMARY KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = tableSchema.primaryKey,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Indexes
            if (tableSchema.indexes.isNotEmpty()) {
                Text(
                    text = "INDEXES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                tableSchema.indexes.forEach { index ->
                    Text(
                        text = "• $index",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No schema available", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DataTableViewer(tableData: TableData) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(verticalScrollState)
                .horizontalScroll(horizontalScrollState)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                tableData.columns.forEach { column ->
                    Box(
                        modifier = Modifier
                            .width(200.dp)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = column.name.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = column.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // Data Rows
            tableData.rows.forEach { row ->
                Row {
                    tableData.columns.forEach { column ->
                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = row[column.name] ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSelectorBottomSheet(
    databases: List<DatabaseInfo>,
    selectedDatabase: DatabaseInfo?,
    onDatabaseSelected: (DatabaseInfo) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Select Database",
                style = MaterialTheme.typography.headlineSmall,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(24.dp))

            databases.forEach { database ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDatabaseSelected(database) }
                        .background(
                            if (database == selectedDatabase) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = database.name,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = database.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                if (database != databases.last()) {
                    AlohomoraHorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
