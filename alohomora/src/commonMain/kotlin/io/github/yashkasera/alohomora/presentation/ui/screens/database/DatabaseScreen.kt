package io.github.yashkasera.alohomora.presentation.ui.screens.database

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTable
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.TableColumn
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DatabaseScreen(
    onBackClick: () -> Unit,
    viewModel: DatabaseViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                // A settings gear used to sit here with an empty onClick. Removed rather than
                // left looking functional — the same dead control that made the iOS console
                // impossible to leave.
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.dimens.margin.xl)
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

            // Database Selector - Compact
            DatabaseSelector(
                selectedDatabase = state.selectedDatabase,
                onClick = { viewModel.toggleDatabaseSelector(true) }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

            // Tables Section - Compact
            TablesSection(
                tables = state.tables,
                selectedTable = state.selectedTable,
                onTableSelected = { viewModel.selectTable(it) }
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

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
private fun DatabaseSelector(
    selectedDatabase: DatabaseInfo?,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = "DATABASE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = MaterialTheme.dimens.stroke.small,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(MaterialTheme.dimens.corner.small)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = MaterialTheme.dimens.margin.md, vertical = MaterialTheme.dimens.margin.sm)
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
                Icon(
                    imageVector = Icons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
            }
        }
    }
}

@Composable
private fun TablesSection(
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

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))

        // Chips, not coloured text. Selection was signalled only by foreground-vs-tertiary
        // colour, which is invisible to anyone who does not already know the convention — and
        // AlohomoraFilterChip is what the trace method filters already use for this.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            tables.forEach { table ->
                AlohomoraFilterChip(
                    label = table,
                    selected = table == selectedTable,
                    onClick = { onTableSelected(table) },
                )
            }
        }
    }
}

@Composable
private fun TabsWithContent(
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
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl)
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
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                    if (pagerState.currentPage == index) {
                        Box(
                            modifier = Modifier
                                .width(MaterialTheme.dimens.margin.xxl)
                                .height(MaterialTheme.dimens.stroke.medium)
                                .background(MaterialTheme.colorScheme.onBackground)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

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
private fun BrowseTabContent(tableData: TableData?) {
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
private fun QueryTabContent(
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
                    width = MaterialTheme.dimens.stroke.small,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(MaterialTheme.dimens.corner.small)
                )
                .padding(MaterialTheme.dimens.margin.sm)
        ) {
            Column {
                AlohomoraTextField(
                    value = queryText,
                    onValueChange = onQueryTextChanged,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    singleLine = false,
                    containerColor = Color.Transparent,
                    borderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
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
                        shape = RoundedCornerShape(MaterialTheme.dimens.corner.small),
                        text = "Run",
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.xs),
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.xs))
                            Text("RUN", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

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
                            MaterialTheme.alohomoraColors.successContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = MaterialTheme.dimens.margin.xs),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs)
                        ) {
                            Icon(
                                imageVector = if (status.success) Icons.Check else Icons.X,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.xs),
                                tint = if (status.success) {
                                    MaterialTheme.alohomoraColors.success
                                } else {
                                    MaterialTheme.colorScheme.error
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

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

            if (queryResults != null) {
                DataTableViewer(tableData = queryResults)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Play,
                        title = "No results yet",
                        subtitle = "Write a query above and run it.",
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemaTabContent(tableSchema: TableSchema?) {
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

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))

            // Table-style layout for columns
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = MaterialTheme.dimens.stroke.small,
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
                            .padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = 6.dp),
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

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

            // Primary Key - Compact
            if (tableSchema.primaryKey != null) {
                Text(
                    text = "PRIMARY KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                Text(
                    text = tableSchema.primaryKey.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.sm)
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
            }

            // Indexes - Compact
            if (tableSchema.indexes.isNotEmpty()) {
                Text(
                    text = "INDEXES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                tableSchema.indexes.forEach { index ->
                    Text(
                        text = "• $index",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = 2.dp)
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
private fun DataTableViewer(tableData: TableData) {
    val columns = tableData.columns.map { TableColumn(it.name, it.type) }
    AlohomoraTable(
        columns = columns,
        rows = tableData.rows
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatabaseSelectorBottomSheet(
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
                .padding(MaterialTheme.dimens.margin.lg)
        ) {
            Text(
                text = "Select Database",
                style = MaterialTheme.typography.titleMedium,
                fontStyle = FontStyle.Italic
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            databases.forEach { database ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDatabaseSelected(database) }
                        .background(
                            if (database == selectedDatabase) {
                                MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.3f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(horizontal = MaterialTheme.dimens.margin.md, vertical = 10.dp)
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

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
        }
    }
}
