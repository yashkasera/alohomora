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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Settings
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Database Selector - Compact
            DatabaseSelector(
                selectedDatabase = state.selectedDatabase,
                onClick = { viewModel.toggleDatabaseSelector(true) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Tables Section - Compact
            TablesSection(
                tables = state.tables,
                selectedTable = state.selectedTable,
                onTableSelected = { viewModel.selectTable(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

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
            text = "DATABASE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(4.dp)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedDatabase?.name ?: "Select Database",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "▼",
                    style = MaterialTheme.typography.bodyMedium
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

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            tables.forEach { table ->
                Text(
                    text = table,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (table == selectedTable) {
                        MaterialTheme.colorScheme.onBackground
                    } else {
                        MaterialTheme.colorScheme.tertiary
                    },
                    modifier = Modifier.clickable { onTableSelected(table) }
                )
            }
        }
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
        // Tabs - Compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
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
                        style = MaterialTheme.typography.labelSmall,
                        color = if (pagerState.currentPage == index) {
                            MaterialTheme.colorScheme.onBackground
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (pagerState.currentPage == index) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(MaterialTheme.colorScheme.onBackground)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

        Spacer(modifier = Modifier.height(8.dp))

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
        // Query Input - Compact
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp)
        ) {
            Column {
                AlohomoraTextField(
                    value = queryText,
                    onValueChange = onQueryTextChanged,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
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
                        text = "SQLite",
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
                        Text("▶ RUN", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Query Results
        Column(modifier = Modifier.weight(0.65f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.labelSmall,
                    fontStyle = FontStyle.Italic
                )

                queryStatus?.let { status ->
                    Surface(
                        color = if (status.success) {
                            Color(0xFF4CAF50).copy(alpha = 0.1f)
                        } else {
                            Color(0xFFF44336).copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (status.success) "✓" else "✗",
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

            Spacer(modifier = Modifier.height(8.dp))

            if (queryResults != null) {
                DataTableViewer(tableData = queryResults)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Run a query to see results", style = MaterialTheme.typography.bodySmall)
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
            // Columns Section - Compact
            Text(
                text = "COLUMNS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Table-style layout for columns
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
            ) {
                tableSchema.columns.forEachIndexed { index, column ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index % 2 == 0) Color.Transparent
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = column.name,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = column.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (index < tableSchema.columns.size - 1) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Primary Key - Compact
            if (tableSchema.primaryKey != null) {
                Text(
                    text = "PRIMARY KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tableSchema.primaryKey,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Indexes - Compact
            if (tableSchema.indexes.isNotEmpty()) {
                Text(
                    text = "INDEXES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                tableSchema.indexes.forEach { index ->
                    Text(
                        text = "• $index",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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

    // Calculate fixed column widths based on name length (with minimum)
    val columnWidths = tableData.columns.map { column ->
        val baseWidth = (column.name.length * 8).coerceAtLeast(60)
        baseWidth.coerceIn(60, 180)
    }

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
                    .height(IntrinsicSize.Min)
            ) {
                tableData.columns.forEachIndexed { index, column ->
                    val width = columnWidths[index]
                    Box(
                        modifier = Modifier
                            .width(width.dp)
                            .fillMaxHeight()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = column.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = column.type,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

            // Data Rows
            tableData.rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(
                            if (rowIndex % 2 == 0) Color.Transparent
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                        .height(IntrinsicSize.Min)
                ) {
                    tableData.columns.forEachIndexed { index, column ->
                        val width = columnWidths[index]
                        Box(
                            modifier = Modifier
                                .width(width.dp)
                                .fillMaxHeight()
                                .border(
                                    width = 0.5.dp,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                )
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = row[column.name] ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowIndex < tableData.rows.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                    )
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
                .padding(16.dp)
        ) {
            Text(
                text = "Select Database",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = database.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = database.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (database != databases.last()) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
