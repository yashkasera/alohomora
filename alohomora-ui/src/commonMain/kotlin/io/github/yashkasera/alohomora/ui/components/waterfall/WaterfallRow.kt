package io.github.yashkasera.alohomora.ui.components.waterfall

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.durationNanos
import io.github.yashkasera.alohomora.common.isError
import io.github.yashkasera.alohomora.common.trace.TraceRow
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.theme.muted
import io.github.yashkasera.alohomora.ui.theme.mutedContainer
import io.github.yashkasera.alohomora.ui.theme.panelBorder

/** Indent per tree level. Small enough that a 6-deep span still has room for its name at phone width. */
private val IndentPerDepth = 12.dp

/** Height of one waterfall row. Dense on purpose: comparing 40 bars means fitting 40 on screen. */
internal val WaterfallRowHeight = 28.dp

/** Test tag prefix for a bar, so the zero-duration invariant is assertable. */
const val WaterfallBarTestTagPrefix: String = "span-bar-"

/**
 * One row of the waterfall: a tree-indented name on the left, a duration bar on the right.
 *
 * The bar is drawn in [drawBehind] rather than positioned with `offset`/`width` modifiers. Modifiers
 * would need the track width in `Dp` at composition time, which forces a `BoxWithConstraints` — and
 * therefore a subcomposition — per row, and re-layout on every scroll. Drawing takes the width from
 * the draw scope for free, and puts the minimum-width and event-tick rules in one place.
 */
@Composable
fun WaterfallRowItem(
    row: TraceRow,
    window: TraceWindow,
    ticks: List<Long>,
    isSelected: Boolean,
    nameFraction: Float,
    onToggleCollapse: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val span = row.span
    val isError = span.isError()
    val barColor = spanBarColor(span.kind, isError, span.isViewed)
    val subtreeColor = spanSubtreeColor(span.kind, isError)
    val gridColor = MaterialTheme.colorScheme.panelBorder
    val eventColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val minBarWidth = MaterialTheme.dimens.stroke.medium
    val cornerRadius = MaterialTheme.dimens.corner.small

    val rowBackground = when {
        isSelected -> MaterialTheme.colorScheme.surfaceVariant
        isError -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(WaterfallRowHeight)
            .background(rowBackground)
            .clickable(onClick = onSelect),
        verticalAlignment = WaterfallRowAlignment,
    ) {
        NameCell(
            row = row,
            isError = isError,
            barColor = barColor,
            onToggleCollapse = onToggleCollapse,
            modifier = Modifier
                .fillMaxWidth(nameFraction)
                // The column guide, drawn rather than overlaid. An overlay spanning the list would
                // consume the pointer events of every row it crossed; drawing is inert.
                .drawBehind {
                    drawLine(
                        color = gridColor,
                        start = Offset(size.width, 0f),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                    )
                },
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .testTag("$WaterfallBarTestTagPrefix${span.spanId}")
                .drawBehind {
                    drawGridlines(window, ticks, gridColor)
                    if (row.isCollapsed) {
                        drawBar(
                            startNanos = row.subtreeStartNanos,
                            endNanos = row.subtreeEndNanos,
                            window = window,
                            color = subtreeColor,
                            minWidthPx = minBarWidth.toPx(),
                            cornerRadiusPx = cornerRadius.toPx(),
                            heightFraction = 0.72f,
                        )
                    }
                    drawBar(
                        startNanos = span.startEpochNanos,
                        // A skewed span reports an end before its start. Rendered as instantaneous at
                        // the start rather than corrected: the row carries a SKEW chip, and quietly
                        // fixing up impossible timestamps hides the bug someone opened this to find.
                        endNanos = maxOf(span.endEpochNanos, span.startEpochNanos),
                        window = window,
                        color = barColor,
                        minWidthPx = minBarWidth.toPx(),
                        cornerRadiusPx = cornerRadius.toPx(),
                        heightFraction = 0.5f,
                    )
                    drawEventTicks(
                        span = span,
                        window = window,
                        color = eventColor,
                        widthPx = minBarWidth.toPx() / 2f,
                    )
                },
        )
        Text(
            text = formatDuration(span.durationNanos()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.muted,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            modifier = Modifier
                .width(DurationColumnWidth)
                .padding(start = MaterialTheme.dimens.margin.sm),
        )
    }
}

/** Fixed so durations right-align into a scannable column instead of ragging with the bar ends. */
private val DurationColumnWidth: Dp = 72.dp

@Composable
private fun NameCell(
    row: TraceRow,
    isError: Boolean,
    barColor: Color,
    onToggleCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(end = MaterialTheme.dimens.margin.sm),
        verticalAlignment = WaterfallRowAlignment,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
    ) {
        Spacer(Modifier.width(IndentPerDepth * row.depth))
        if (row.hasChildren) {
            Icon(
                imageVector = if (row.isCollapsed) Icons.ChevronRight else Icons.ChevronDown,
                contentDescription = if (row.isCollapsed) "Expand" else "Collapse",
                tint = MaterialTheme.colorScheme.muted,
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.md)
                    .clickable(onClick = onToggleCollapse),
            )
        } else {
            // Keeps sibling names aligned whether or not they have children; without it every leaf
            // shifts left of its expandable siblings and the tree stops reading as a tree.
            Spacer(Modifier.width(MaterialTheme.dimens.icon.md))
        }
        Box(
            Modifier
                .size(MaterialTheme.dimens.icon.xs / 2)
                .background(barColor, MaterialTheme.shapes.extraSmall),
        )
        Text(
            text = row.span.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (row.span.isViewed) FontWeight.Normal else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (row.isCollapsed && row.descendantCount > 0) {
            AlohomoraChip(
                label = "+${row.descendantCount}",
                uppercase = false,
                containerColor = MaterialTheme.colorScheme.mutedContainer,
                contentColor = MaterialTheme.colorScheme.muted,
            )
        }
        if (isError) {
            AlohomoraChip(
                label = "error",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        // Orphan and skew are surfaced, never smoothed over: an orphan means the parent has not
        // arrived (or never will), and skew means the timestamps are impossible. Both change how the
        // bar should be read.
        if (row.isOrphan) {
            AlohomoraChip(
                label = "orphan",
                containerColor = MaterialTheme.colorScheme.mutedContainer,
                contentColor = MaterialTheme.colorScheme.muted,
            )
        }
        if (row.hasSkew) {
            AlohomoraChip(
                label = "skew",
                containerColor = MaterialTheme.colorScheme.mutedContainer,
                contentColor = MaterialTheme.colorScheme.muted,
            )
        }
    }
}
