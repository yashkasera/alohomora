package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DatabaseViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraTable
import io.github.yashkasera.alohomora.ui.components.TableColumn

@Composable
fun DatabasePanel(databaseViewModel: DatabaseViewModel) {
    val uiState by databaseViewModel.uiState.collectAsState()
    val snapshot = uiState.snapshot
    val schema = snapshot.schema
    val tableSnapshot =
        snapshot.table?.takeIf { it.databaseName == snapshot.selectedDatabase?.name }
    val databases = snapshot.databases
    val selectedDatabase = snapshot.selectedDatabase
    val columns by remember {
        derivedStateOf {
            snapshot.schema?.schemas?.firstOrNull { it.name == tableSnapshot?.name }?.columns?.map { tableColumn ->
                TableColumn(
                    name = tableColumn.name,
                    type = tableColumn.type,
                )
            }.orEmpty()
        }
    }
    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Databases",
                subtitle = "Manage your app databases",
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .padding(it)
                .padding(horizontal = MaterialTheme.dimens.margin.xxl, vertical = MaterialTheme.dimens.margin.md)
                .fillMaxSize(),
        ) {
            Text(text = "Databases", style = MaterialTheme.typography.titleLarge)
            if (databases.isEmpty()) {
                EmptyState(
                    icon = Icons.Database,
                    title = "No databases",
                    subtitle = "The connected app has no inspectable databases.",
                )
                return@Column
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    databases.forEach { database ->
                        AlohomoraFilterChip(
                            label = database.name,
                            selected = database == selectedDatabase,
                            onClick = {
                                databaseViewModel.selectDatabase(database.name)
                            },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
            if (selectedDatabase == null) {
                // Not an error state — nothing has gone wrong, the user simply has not picked
                // one yet. It was rendered in error red.
                EmptyState(
                    icon = Icons.Database,
                    title = "Select a database",
                    subtitle = "Choose one above to browse its tables.",
                )
                return@Column
            }
            val tables =
                if (schema?.databaseName == selectedDatabase?.name) schema?.tables.orEmpty() else emptyList()
            Text(text = "Tables", style = MaterialTheme.typography.titleLarge)
            if (tables.isEmpty()) {
                Text(
                    text = "No tables found in this database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            } else {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tables.forEach { table ->
                        AlohomoraFilterChip(
                            label = table,
                            selected = table == snapshot.table?.name,
                            onClick = onClick@{
                                val dbName = selectedDatabase.name ?: return@onClick
                                databaseViewModel.requestTable(dbName, table)
                            },
                        )

                    }
                }
            }
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
            Text(text = "Rows", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
            if (tableSnapshot == null) {
                Text(text = "Select a table to load rows.")
            } else {
                AlohomoraTable(
                    columns = columns,
                    rows = tableSnapshot.rows,
                )
            }
        }
    }
}
