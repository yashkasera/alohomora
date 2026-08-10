package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * A data table component with proper column alignment across all rows.
 * Column widths are calculated based on intrinsic measurements of all content.
 *
 * @param columns List of column definitions with name and type
 * @param rows List of row data as maps from column name to value
 * @param modifier Modifier for the table container
 */
@Composable
fun AlohomoraTable(
    columns: List<TableColumn>,
    rows: List<Map<String, String?>>,
    modifier: Modifier = Modifier
) {
    if (columns.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No columns",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
        return
    }

    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    // Calculate column widths based on content
    val columnWidths = remember(columns, rows) {
        calculateColumnWidths(columns, rows)
    }

    Box(
        modifier = modifier
            .border(
                width = MaterialTheme.dimens.stroke.small,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small
            )
            .clip(MaterialTheme.shapes.small)
            .verticalScroll(verticalScrollState)
            .horizontalScroll(horizontalScrollState)
    ) {
        Column {
            // Header row
            TableRow(
                cells = columns.map { column ->
                    CellContent.Header(
                        name = column.name,
                        type = column.type
                    )
                },
                columnWidths = columnWidths,
                isHeader = true
            )

            // Data rows
            rows.forEachIndexed { index, rowData ->
                TableRow(
                    cells = columns.map { column ->
                        CellContent.Data(
                            value = rowData[column.name] ?: ""
                        )
                    },
                    columnWidths = columnWidths,
                    isHeader = false,
                    rowIndex = index
                )
            }
        }
    }
}

private sealed class CellContent {
    data class Header(val name: String, val type: String) : CellContent()
    data class Data(val value: String) : CellContent()
}

@Composable
private fun TableRow(
    cells: List<CellContent>,
    columnWidths: List<Int>,
    isHeader: Boolean,
    rowIndex: Int = 0
) {
    Row(
        modifier = Modifier
            .background(
                if (isHeader) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else if (rowIndex % 2 == 0) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                }
            )
    ) {
        cells.forEachIndexed { index, cell ->
            val width = columnWidths.getOrElse(index) { 80 }
            TableCell(
                content = cell,
                width = width
            )
        }
    }
}

@Composable
private fun TableCell(
    content: CellContent,
    width: Int
) {

    Box(
        modifier = Modifier
            .width(width.dp)
            .border(
                width = MaterialTheme.dimens.stroke.thin,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            .padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        when (content) {
            is CellContent.Header -> {
                Column {
                    Text(
                        text = content.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = content.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            is CellContent.Data -> {
                Text(
                    text = content.value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Calculate optimal column widths based on content.
 * Uses a heuristic that considers both header and data content.
 */
private fun calculateColumnWidths(
    columns: List<TableColumn>,
    rows: List<Map<String, String?>>
): List<Int> {
    // Base widths on header name length (in dp, approx 8dp per char + padding)
    val baseWidths = columns.map { column ->
        val nameWidth = column.name.length * 8 + 24 // chars + padding
        val typeWidth = column.type.length * 7 + 24
        maxOf(nameWidth, typeWidth, 80) // minimum 80dp
    }

    // Check data content for each column and expand if needed
    val dataWidths = columns.map { column ->
        var maxDataWidth = 0
        rows.forEach { row ->
            val value = row[column.name] ?: ""
            // Estimate width: ~7dp per character for monospace, max ~150dp
            val estimatedWidth = (value.length * 7).coerceAtMost(150) + 24
            maxDataWidth = maxOf(maxDataWidth, estimatedWidth)
        }
        maxDataWidth
    }

    // Combine: take max of header width and data width, with min/max constraints
    return baseWidths.mapIndexed { index, baseWidth ->
        val dataWidth = dataWidths[index]
        maxOf(baseWidth, dataWidth).coerceIn(60, 200)
    }
}

/**
 * Column definition for the table.
 */
data class TableColumn(
    val name: String,
    val type: String
)
