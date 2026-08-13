package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun AlohomoraTable(
    columns: List<TableColumn>,
    rows: List<Map<String, String?>>,
    modifier: Modifier = Modifier,
    onCellEdit: ((rowIndex: Int, columnName: String, newValue: String?) -> Unit)? = null,
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

    val baseWidths = remember(columns, rows) {
        calculateColumnWidths(columns, rows)
    }

    var editingCell by remember { mutableStateOf<EditingCell?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .border(
                width = MaterialTheme.dimens.stroke.small,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = MaterialTheme.shapes.small
            )
            .clip(MaterialTheme.shapes.small)
    ) {
        val density = LocalDensity.current
        val availableWidthPx = with(density) { maxWidth.toPx() }
        val totalBasePx = baseWidths.sumOf { it }
        val columnWidthsDp = if (totalBasePx < availableWidthPx && baseWidths.isNotEmpty()) {
            val scale = availableWidthPx / totalBasePx
            baseWidths.map { with(density) { (it * scale).toInt().toDp() } }
        } else {
            baseWidths.map { it.dp }
        }

        val scrollModifier = if (totalBasePx >= availableWidthPx) {
            Modifier.horizontalScroll(horizontalScrollState)
        } else {
            Modifier.fillMaxWidth()
        }

        Box(
            modifier = Modifier
                .verticalScroll(verticalScrollState)
                .then(scrollModifier)
        ) {
            Column {
                TableRow(
                    cells = columns.map { column ->
                        CellContent.Header(name = column.name, type = column.type)
                    },
                    columnWidths = columnWidthsDp,
                    isHeader = true,
                    fillWidth = totalBasePx < availableWidthPx,
                )

                rows.forEachIndexed { rowIndex, rowData ->
                    TableRow(
                        cells = columns.map { column ->
                            CellContent.Data(value = rowData[column.name] ?: "")
                        },
                        columnWidths = columnWidthsDp,
                        isHeader = false,
                        rowIndex = rowIndex,
                        columns = columns,
                        editingCell = editingCell,
                        onCellDoubleClick = if (onCellEdit != null) { colIndex ->
                            val col = columns[colIndex]
                            if (col.editable) {
                                editingCell = EditingCell(
                                    rowIndex = rowIndex,
                                    colIndex = colIndex,
                                    originalValue = rowData[col.name] ?: "",
                                )
                            }
                        } else null,
                        onEditCommit = { colIndex, newValue ->
                            val current = editingCell
                            if (current != null && current.rowIndex == rowIndex && current.colIndex == colIndex) {
                                editingCell = null
                            }
                            val col = columns[colIndex]
                            val oldValue = rowData[col.name] ?: ""
                            if (newValue != oldValue) {
                                onCellEdit?.invoke(
                                    rowIndex,
                                    col.name,
                                    newValue.ifEmpty { null },
                                )
                            }
                        },
                        onEditCancel = {
                            val current = editingCell
                            if (current != null && current.rowIndex == rowIndex) {
                                editingCell = null
                            }
                        },
                        fillWidth = totalBasePx < availableWidthPx,
                    )
                }
            }
        }
    }
}

private data class EditingCell(
    val rowIndex: Int,
    val colIndex: Int,
    val originalValue: String,
)

private sealed class CellContent {
    data class Header(val name: String, val type: String) : CellContent()
    data class Data(val value: String) : CellContent()
}

@Composable
private fun TableRow(
    cells: List<CellContent>,
    columnWidths: List<Dp>,
    isHeader: Boolean,
    rowIndex: Int = 0,
    columns: List<TableColumn> = emptyList(),
    editingCell: EditingCell? = null,
    onCellDoubleClick: ((colIndex: Int) -> Unit)? = null,
    onEditCommit: ((colIndex: Int, newValue: String) -> Unit)? = null,
    onEditCancel: (() -> Unit)? = null,
    fillWidth: Boolean = false,
) {
    Row(
        modifier = Modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
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
            val width = columnWidths.getOrElse(index) { 80.dp }
            val isEditing = !isHeader && editingCell != null
                    && editingCell.rowIndex == rowIndex
                    && editingCell.colIndex == index

            val widthModifier = if (fillWidth && index == cells.lastIndex) {
                Modifier.weight(1f)
            } else {
                Modifier.width(width)
            }

            if (isEditing && cell is CellContent.Data) {
                EditableCell(
                    initialValue = editingCell.originalValue,
                    onCommit = { onEditCommit?.invoke(index, it) },
                    onCancel = { onEditCancel?.invoke() },
                    modifier = widthModifier,
                )
            } else {
                TableCell(
                    content = cell,
                    modifier = widthModifier,
                    editable = !isHeader && columns.getOrNull(index)?.editable == true
                            && onCellDoubleClick != null,
                    onDoubleClick = if (!isHeader) {
                        { onCellDoubleClick?.invoke(index) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun TableCell(
    content: CellContent,
    modifier: Modifier,
    editable: Boolean = false,
    onDoubleClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .border(
                width = MaterialTheme.dimens.stroke.thin,
                color = MaterialTheme.colorScheme.outlineVariant
            )
            .then(
                if (onDoubleClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onDoubleClick() })
                    }
                } else Modifier
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EditableCell(
    initialValue: String,
    onCommit: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier,
) {
    var text by remember(initialValue) { mutableStateOf(initialValue) }
    val focusRequester = remember { FocusRequester() }
    var committed by remember(initialValue) { mutableStateOf(false) }
    var hasFocused by remember(initialValue) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = MaterialTheme.dimens.margin.sm, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = TextStyle(
                fontSize = MaterialTheme.typography.bodySmall.fontSize,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { state ->
                    if (hasFocused && !state.isFocused && !committed) {
                        committed = true
                        onCommit(text)
                    }
                    if (state.isFocused) hasFocused = true
                }
                .onPreviewKeyEvent { event ->
                    when (event.key) {
                        Key.Enter -> {
                            if (!committed) {
                                committed = true
                                onCommit(text)
                            }
                            true
                        }
                        Key.Escape -> {
                            if (!committed) {
                                committed = true
                                onCancel()
                            }
                            true
                        }
                        else -> false
                    }
                },
        )
    }

    LaunchedEffect(initialValue) {
        focusRequester.requestFocus()
    }
}

private fun calculateColumnWidths(
    columns: List<TableColumn>,
    rows: List<Map<String, String?>>
): List<Int> {
    val baseWidths = columns.map { column ->
        val nameWidth = column.name.length * 8 + 24
        val typeWidth = column.type.length * 7 + 24
        maxOf(nameWidth, typeWidth, 80)
    }

    val dataWidths = columns.map { column ->
        var maxDataWidth = 0
        rows.forEach { row ->
            val value = row[column.name] ?: ""
            val estimatedWidth = (value.length * 7).coerceAtMost(150) + 24
            maxDataWidth = maxOf(maxDataWidth, estimatedWidth)
        }
        maxDataWidth
    }

    return baseWidths.mapIndexed { index, baseWidth ->
        val dataWidth = dataWidths[index]
        maxOf(baseWidth, dataWidth).coerceIn(60, 200)
    }
}

data class TableColumn(
    val name: String,
    val type: String,
    val editable: Boolean = true,
)
