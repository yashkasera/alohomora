package io.github.yashkasera.alohomora.presentation.ui.screens.database

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.common.DatabaseInfo
import io.github.yashkasera.alohomora.common.TableData
import io.github.yashkasera.alohomora.common.TableSchema
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.AlohomoraTable
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.TableColumn
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DatabaseScreen(
    onBackClick: () -> Unit,
    viewModel: DatabaseViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Database",
                subtitle = null,
                navigationIcon = {
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
                        Icon(Icons.ArrowLeft, contentDescription = "Back")
                    }
                },
                // A settings gear used to sit here with an empty onClick. Removed rather than
                // left looking functional — the same dead control that made the iOS console
                // impossible to leave.
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = MaterialTheme.dimens.margin.xl),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

            DatabaseSelector(
                selectedDatabase = state.selectedDatabase,
                onClick = { viewModel.toggleDatabaseSelector(true) },
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

            TablesSection(
                tables = state.tables,
                selectedTable = state.selectedTable,
                onTableSelected = { viewModel.selectTable(it) },
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

            TabsWithContent(
                currentTab = state.currentTab,
                onTabSelected = { viewModel.selectTab(it) },
                queryText = state.queryText,
                onQueryTextChanged = { viewModel.updateQueryText(it) },
                onRunQuery = { viewModel.executeQuery() },
                queryResults = state.queryResults,
                queryStatus = state.queryStatus,
                tableData = state.tableData,
                tableSchema = state.tableSchema,
            )
        }

        if (state.showDatabaseSelector) {
            DatabaseSelectorBottomSheet(
                databases = state.databases,
                selectedDatabase = state.selectedDatabase,
                onDatabaseSelected = { viewModel.selectDatabase(it) },
                onDismiss = { viewModel.toggleDatabaseSelector(false) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DatabaseSelector(
    selectedDatabase: DatabaseInfo?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.shapes.large,
            )
            .clickable(onClick = onClick)
            .testTag(AlohomoraTestTags.Database.SELECTOR)
            .padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Box(
            modifier = Modifier
                .size(MaterialTheme.dimens.icon.xl)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    MaterialShapes.Cookie6Sided.toShape(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Database,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "DATABASE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = selectedDatabase?.name ?: "Select a database",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.ChevronDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
        )
    }
}

@Composable
private fun TablesSection(
    tables: List<String>,
    selectedTable: String?,
    onTableSelected: (String) -> Unit,
) {
    Column(modifier = Modifier.testTag(AlohomoraTestTags.Database.TABLES)) {
        Text(
            text = "TABLES",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
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
                    modifier = Modifier.testTag(AlohomoraTestTags.Database.table(table)),
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
    tableSchema: TableSchema?,
) {
    val tabs = listOf("BROWSE", "QUERY", "SCHEMA")
    val pagerState = rememberPagerState(initialPage = currentTab, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    // `settledPage`, not `currentPage`, and the guard is not redundant — together they break a
    // feedback loop that made a two-page jump land on the wrong tab.
    //
    // `currentPage` tracks the *closest* page mid-scroll, so animating BROWSE -> SCHEMA passes
    // through QUERY. That fired `onTabSelected(1)`, which set `currentTab` to 1, which re-ran the
    // effect below and animated back to QUERY — so tapping SCHEMA from BROWSE simply never got
    // there. Only a one-page move was ever unaffected, which is why it looked fine in use.
    LaunchedEffect(currentTab) {
        if (pagerState.currentPage != currentTab) {
            pagerState.animateScrollToPage(currentTab)
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        onTabSelected(pagerState.settledPage)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AlohomoraPrimaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(AlohomoraTestTags.Database.TABS),
        ) {
            tabs.forEachIndexed { index, tab ->
                AlohomoraTab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = tab,
                    modifier = Modifier.testTag(AlohomoraTestTags.Database.tab(tab)),
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> BrowseTabContent(tableData = tableData)
                1 -> QueryTabContent(
                    queryText = queryText,
                    onQueryTextChanged = onQueryTextChanged,
                    onRunQuery = onRunQuery,
                    queryResults = queryResults,
                    queryStatus = queryStatus,
                )

                2 -> SchemaTabContent(tableSchema = tableSchema)
            }
        }
    }
}

@Composable
private fun BrowseTabContent(tableData: TableData?) {
    if (tableData != null) {
        DataTableViewer(
            tableData = tableData,
            modifier = Modifier.testTag(AlohomoraTestTags.Database.BROWSE),
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(AlohomoraTestTags.Database.BROWSE),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Database,
                title = "No data",
                subtitle = "Select a table to browse its rows.",
            )
        }
    }
}

@Composable
private fun QueryTabContent(
    queryText: String,
    onQueryTextChanged: (String) -> Unit,
    onRunQuery: () -> Unit,
    queryResults: TableData?,
    queryStatus: QueryStatus?,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Query editor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.35f)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.large,
                )
                .padding(MaterialTheme.dimens.margin.md),
        ) {
            AlohomoraTextField(
                value = queryText,
                onValueChange = onQueryTextChanged,
                placeholder = "SELECT * FROM …",
                modifier = Modifier.fillMaxWidth().weight(1f)
                    .testTag(AlohomoraTestTags.Database.QUERY_EDITOR),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = false,
                containerColor = Color.Transparent,
                borderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraChip(label = "SQLite")

                AlohomoraFilledButton(
                    text = "Run",
                    onClick = onRunQuery,
                    modifier = Modifier.testTag(AlohomoraTestTags.Database.QUERY_RUN),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Play,
                            contentDescription = null,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

        Column(
            modifier = Modifier
                .weight(0.65f)
                .testTag(AlohomoraTestTags.Database.QUERY_RESULT),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "RESULTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                queryStatus?.let { status -> QueryStatusChip(status) }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

            if (queryResults != null) {
                DataTableViewer(tableData = queryResults)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
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
private fun QueryStatusChip(status: QueryStatus) {
    val accent = if (status.success) {
        MaterialTheme.alohomoraColors.success
    } else {
        MaterialTheme.colorScheme.error
    }
    Row(
        modifier = Modifier
            .testTag(AlohomoraTestTags.Database.QUERY_STATUS)
            .background(accent.copy(alpha = 0.12f), MaterialTheme.shapes.small)
            .padding(
                horizontal = MaterialTheme.dimens.margin.sm,
                vertical = MaterialTheme.dimens.margin.xs,
            ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (status.success) Icons.Check else Icons.X,
            contentDescription = if (status.success) "Success" else "Failed",
            modifier = Modifier.size(MaterialTheme.dimens.icon.xs),
            tint = accent,
        )
        Text(
            text = "${status.executionTimeMs}ms",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
        )
    }
}

@Composable
private fun SchemaTabContent(tableSchema: TableSchema?) {
    if (tableSchema != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .testTag(AlohomoraTestTags.Database.SCHEMA),
        ) {
            Text(
                text = "COLUMNS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.shapes.large,
                    )
                    .padding(vertical = MaterialTheme.dimens.margin.xs),
            ) {
                tableSchema.columns.forEachIndexed { index, column ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.dimens.margin.md,
                                vertical = MaterialTheme.dimens.margin.sm,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        ) {
                            if (column.name == tableSchema.primaryKey) {
                                Icon(
                                    imageVector = Icons.Key,
                                    contentDescription = "Primary key",
                                    tint = MaterialTheme.alohomoraColors.accent,
                                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                                )
                            }
                            Text(
                                text = column.name,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        AlohomoraChip(label = column.type)
                    }
                    if (index < tableSchema.columns.size - 1) {
                        AlohomoraHorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

            if (tableSchema.primaryKey != null) {
                Text(
                    text = "PRIMARY KEY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                Text(
                    text = tableSchema.primaryKey.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.sm),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
            }

            if (tableSchema.indexes.isNotEmpty()) {
                Text(
                    text = "INDEXES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                tableSchema.indexes.forEach { index ->
                    Text(
                        text = "• $index",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(
                            horizontal = MaterialTheme.dimens.margin.sm,
                            vertical = 2.dp,
                        ),
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(AlohomoraTestTags.Database.SCHEMA),
            contentAlignment = Alignment.Center,
        ) {
            EmptyState(
                icon = Icons.Layers,
                title = "No schema available",
                subtitle = "Select a table to inspect its columns.",
            )
        }
    }
}

@Composable
private fun DataTableViewer(
    tableData: TableData,
    modifier: Modifier = Modifier,
) {
    val columns = tableData.columns.map { TableColumn(it.name, it.type) }
    AlohomoraTable(
        columns = columns,
        rows = tableData.rows,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DatabaseSelectorBottomSheet(
    databases: List<DatabaseInfo>,
    selectedDatabase: DatabaseInfo?,
    onDatabaseSelected: (DatabaseInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg)
                .testTag(AlohomoraTestTags.Database.SELECTOR_SHEET),
        ) {
            Text(
                text = "Select database",
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            databases.forEach { database ->
                val isSelected = database == selectedDatabase
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                Color.Transparent
                            },
                            MaterialTheme.shapes.medium,
                        )
                        .clickable { onDatabaseSelected(database) }
                        .testTag(AlohomoraTestTags.Database.database(database.name))
                        .padding(
                            horizontal = MaterialTheme.dimens.margin.md,
                            vertical = MaterialTheme.dimens.margin.sm,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                ) {
                    Box(
                        modifier = Modifier
                            .size(MaterialTheme.dimens.icon.xl)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                MaterialShapes.Cookie6Sided.toShape(),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Database,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = database.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = database.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.alohomoraColors.accent,
                            modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                        )
                    }
                }

                if (database != databases.last()) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
        }
    }
}
