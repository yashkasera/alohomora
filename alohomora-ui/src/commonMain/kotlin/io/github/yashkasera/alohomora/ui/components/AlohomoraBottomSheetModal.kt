package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AlohomoraBottomSheetShape
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Default values for [AlohomoraBottomSheetModal].
 */
object AlohomoraBottomSheetDefaults {
    /** The default shape for the bottom sheet (top corners rounded). */
    val shape: Shape get() = AlohomoraBottomSheetShape

    /** The default elevation for the bottom sheet. */
    val tonalElevation: Dp = 0.dp

    /** The default height for the drag handle. */
    val dragHandleHeight: Dp = 24.dp

    /** The default width for the drag handle. */
    val dragHandleWidth: Dp = 32.dp

    /** The default corner radius for the drag handle. */
    val dragHandleCornerRadius: Dp = 4.dp

    /** The default horizontal padding for content. */
    val contentHorizontalPadding: Dp = 24.dp

    /** The default bottom padding for content. */
    val contentBottomPadding: Dp = 32.dp
}

/**
 * Configuration for the [AlohomoraBottomSheetModal] drag handle.
 *
 * @property visible Whether to show the drag handle
 * @property color The color of the drag handle
 * @property width The width of the drag handle
 * @property height The height of the drag handle
 * @property cornerRadius The corner radius of the drag handle
 */
data class DragHandleConfig(
    val visible: Boolean = true,
    val color: Color? = null,
    val width: Dp = AlohomoraBottomSheetDefaults.dragHandleWidth,
    val height: Dp = AlohomoraBottomSheetDefaults.dragHandleHeight,
    val cornerRadius: Dp = AlohomoraBottomSheetDefaults.dragHandleCornerRadius,
)

/**
 * A wrapper around [ModalBottomSheet] that provides consistent styling,
 * shapes, drag handles, and behaviors for all bottom sheets in the app.
 *
 * @param onDismissRequest Callback when the user attempts to dismiss the sheet
 * @param modifier Modifier to be applied to the bottom sheet
 * @param sheetState State of the sheet
 * @param shape The shape of the bottom sheet
 * @param containerColor The background color of the bottom sheet
 * @param contentColor The preferred content color for the bottom sheet
 * @param tonalElevation The tonal elevation of the bottom sheet
 * @param dragHandle Configuration for the drag handle
 * @param skipPartiallyExpanded Whether to skip the partially expanded state
 * @param content The content of the bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlohomoraBottomSheetModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
    ),
    shape: Shape = AlohomoraBottomSheetDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = AlohomoraBottomSheetDefaults.tonalElevation,
    dragHandle: DragHandleConfig = DragHandleConfig(),
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        dragHandle = {
            if (dragHandle.visible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dragHandle.height)
                        .padding(vertical = MaterialTheme.dimens.margin.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(dragHandle.width)
                            .height(dragHandle.height)
                            .background(
                                color = dragHandle.color
                                    ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(dragHandle.cornerRadius),
                            ),
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AlohomoraBottomSheetDefaults.contentHorizontalPadding)
                .padding(bottom = AlohomoraBottomSheetDefaults.contentBottomPadding),
            content = content,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlohomoraBottomSheetModal(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    skipPartiallyExpanded: Boolean = true,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
    ),
    shape: Shape = AlohomoraBottomSheetDefaults.shape,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = AlohomoraBottomSheetDefaults.tonalElevation,
    dragHandle: DragHandleConfig = DragHandleConfig(),
    horizontalPadding: Dp = AlohomoraBottomSheetDefaults.contentHorizontalPadding,
    bottomPadding: Dp = AlohomoraBottomSheetDefaults.contentBottomPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        dragHandle = {
            if (dragHandle.visible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dragHandle.height)
                        .padding(vertical = MaterialTheme.dimens.margin.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(dragHandle.width)
                            .height(dragHandle.height)
                            .background(
                                color = dragHandle.color
                                    ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(dragHandle.cornerRadius),
                            ),
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = bottomPadding),
            content = content,
        )
    }
}
