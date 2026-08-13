package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraTable
import io.github.yashkasera.alohomora.ui.components.TableColumn
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.theme.dimens
import java.awt.Cursor

@Composable
fun DatabasePanel(databaseViewModel: DatabaseViewModel) {
    val uiState by databaseViewModel.uiState.collectAsState()
    val snapshot = uiState.snapshot
    val schema = snapshot.schema
    val tableSnapshot =
        snapshot.table?.takeIf { it.databaseName == snapshot.selectedDatabase?.name }
    val databases = snapshot.databases
    val selectedDatabase = snapshot.selectedDatabase
    val tables =
        if (schema?.databaseName == selectedDatabase?.name) schema?.tables.orEmpty() else emptyList()

    val tableSchema = remember(schema, tableSnapshot?.name) {
        schema?.schemas?.firstOrNull { it.name == tableSnapshot?.name }
    }
    val columns = remember(tableSchema) {
        tableSchema?.columns?.map { col ->
            TableColumn(name = col.name, type = col.type, editable = !col.primaryKey)
        }.orEmpty()
    }
    val primaryKeyColumns = remember(tableSchema) {
        tableSchema?.columns?.filter { it.primaryKey }?.map { it.name }.orEmpty()
    }

    var expandedDb by remember { mutableStateOf<String?>(null) }
    var sidebarFraction by remember { mutableStateOf(SIDEBAR_DEFAULT) }

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Databases",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = buildSubtitle(databases.size, tables.size, tableSnapshot?.rows?.size),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        if (databases.isEmpty()) {
            EmptyState(
                icon = Icons.Database,
                title = "No databases",
                subtitle = "The connected app has no inspectable databases.",
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val totalWidthPx = with(LocalDensity.current) { maxWidth.toPx() }

            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(sidebarFraction)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    databases.forEach { database ->
                        val isExpanded = expandedDb == database.name
                        val childTables =
                            if (isExpanded && schema?.databaseName == database.name) schema.tables else emptyList()

                        item(key = "db:${database.name}") {
                            DatabaseHeader(
                                name = database.name,
                                tableCount = childTables.size,
                                expanded = isExpanded,
                                onClick = {
                                    if (isExpanded) {
                                        expandedDb = null
                                    } else {
                                        expandedDb = database.name
                                        databaseViewModel.selectDatabase(database.name)
                                    }
                                },
                            )
                        }

                        if (isExpanded) {
                            items(childTables, key = { "table:${database.name}:$it" }) { table ->
                                TableItem(
                                    name = table,
                                    selected = table == tableSnapshot?.name
                                            && database.name == tableSnapshot.databaseName,
                                    onClick = {
                                        databaseViewModel.requestTable(database.name, table)
                                    },
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .width(DIVIDER_WIDTH)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outlineVariant)
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .pointerInput(totalWidthPx) {
                            detectDragGestures { _, dragAmount ->
                                val delta = dragAmount.x / totalWidthPx
                                sidebarFraction = (sidebarFraction + delta)
                                    .coerceIn(SIDEBAR_MIN, SIDEBAR_MAX)
                            }
                        },
                )

                Box(
                    modifier = Modifier
                        .weight(1f - sidebarFraction)
                        .padding(MaterialTheme.dimens.margin.md)
                        .fillMaxHeight(),
                ) {
                    if (tableSnapshot == null) {
                        EmptyState(
                            icon = Icons.Layers,
                            title = "No table selected",
                            subtitle = "Pick a database and table from the sidebar to browse rows.",
                        )
                    } else {
                        AlohomoraTable(
                            columns = columns,
                            rows = tableSnapshot.rows,
                            modifier = Modifier.fillMaxSize(),
                            onCellEdit = onCellEdit@{ rowIndex, columnName, newValue ->
                                val dbName = tableSnapshot.databaseName ?: return@onCellEdit
                                val row = tableSnapshot.rows.getOrNull(rowIndex) ?: return@onCellEdit
                                val pkMap = primaryKeyColumns.associateWith { pk ->
                                    row[pk] ?: return@onCellEdit
                                }
                                if (pkMap.isEmpty()) return@onCellEdit
                                databaseViewModel.updateCell(
                                    databaseName = dbName,
                                    tableName = tableSnapshot.name,
                                    primaryKeys = pkMap,
                                    columnName = columnName,
                                    newValue = newValue,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DatabaseHeader(
    name: String,
    tableCount: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (expanded) Icons.ChevronDown else Icons.ChevronRight,
            contentDescription = if (expanded) "Collapse" else "Expand",
            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.xs))
        Icon(
            imageVector = Icons.Database,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (expanded && tableCount > 0) {
            Text(
                text = "$tableCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun TableItem(
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .padding(start = MaterialTheme.dimens.margin.xxl)
            .padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(MaterialTheme.dimens.stroke.medium)
                .height(MaterialTheme.dimens.icon.sm)
                .background(
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerLow,
                ),
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
        Icon(
            imageVector = Icons.Layers,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimens.icon.xs),
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun buildSubtitle(dbCount: Int, tableCount: Int, rowCount: Int?): String = buildString {
    append("$dbCount database")
    if (dbCount != 1) append("s")
    if (tableCount > 0) {
        append(" · $tableCount table")
        if (tableCount != 1) append("s")
    }
    if (rowCount != null) {
        append(" · $rowCount row")
        if (rowCount != 1) append("s")
    }
}

private const val SIDEBAR_DEFAULT = 0.25f
private const val SIDEBAR_MIN = 0.15f
private const val SIDEBAR_MAX = 0.5f
private val DIVIDER_WIDTH = 4.dp
