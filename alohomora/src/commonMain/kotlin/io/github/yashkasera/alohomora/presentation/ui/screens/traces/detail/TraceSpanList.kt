package io.github.yashkasera.alohomora.presentation.ui.screens.traces.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.durationNanos
import io.github.yashkasera.alohomora.common.isError
import io.github.yashkasera.alohomora.common.trace.TraceRow
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.barGeometry
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.waterfall.spanBarColor
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.dimens

/** Indent per tree level. Deliberately tight: a 6-deep span still needs room for its name at 360dp. */
private val IndentPerDepth = 10.dp

/**
 * Minimum width of the mini bar lane.
 *
 * Narrow on purpose in portrait. A full waterfall does not fit a phone — with a 40% name column you get
 * roughly 220dp of track, a 5-deep span loses its name entirely to indentation, and there is no hover to
 * recover it. This lane keeps the one thing the waterfall actually provides — *where* in the trace a span
 * sat and *how much* of it it occupied — without needing a legible time axis.
 *
 * A floor rather than a fixed width: in landscape a 72dp lane next to a ~700dp name column wasted the
 * one dimension rotation buys, so the lane scales with the viewport via [traceSpanLaneWidth].
 */
private val MinBarLaneWidth = 72.dp

/** Fraction of the viewport the lane takes once wider than the [MinBarLaneWidth] floor allows. */
private const val BAR_LANE_FRACTION = 0.22f

/** Lane width for the current viewport: a fifth-ish of the width, never below [MinBarLaneWidth]. */
internal fun traceSpanLaneWidth(viewportWidth: Dp): Dp =
    maxOf(MinBarLaneWidth, viewportWidth * BAR_LANE_FRACTION)

/**
 * Corner radius of a span bar, in [Dp] because it is fed to a draw call rather than a `shape =` slot.
 *
 * Deliberately not a theme token, and deliberately duplicated from the shared waterfall's constant:
 * `MaterialTheme.shapes` holds `Shape`s, which a draw scope cannot consume.
 */
private val SpanBarCornerRadius: Dp = 4.dp

/**
 * One span as a phone-width row: tree-indented name, duration, and a mini bar lane.
 *
 * Reuses `barGeometry` and `spanBarColor` from the shared waterfall rather than reimplementing the
 * position/minimum-width rules — the same arithmetic keeps a sub-millisecond span visible here.
 */
@Composable
internal fun TraceSpanRow(
    row: TraceRow,
    window: TraceWindow,
    isSelected: Boolean,
    onToggleCollapse: () -> Unit,
    onSelect: () -> Unit,
    laneWidth: Dp = MinBarLaneWidth,
) {
    val span = row.span
    val isError = span.isError()
    val barColor = spanBarColor(span.kind, isError, span.isViewed)
    val laneColor = MaterialTheme.colorScheme.outlineVariant
    val minBarWidth = MaterialTheme.dimens.stroke.medium
    val cornerRadius = SpanBarCornerRadius

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.TraceDetails.span(span.spanId))
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
            )
            .clickable(onClick = onSelect)
            .padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        Spacer(Modifier.width(IndentPerDepth * row.depth))

        if (row.hasChildren) {
            Icon(
                imageVector = if (row.isCollapsed) Icons.ChevronRight else Icons.ChevronDown,
                contentDescription = if (row.isCollapsed) "Expand" else "Collapse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.md)
                    .testTag(AlohomoraTestTags.TraceDetails.spanCollapse(span.spanId))
                    .clickable(onClick = onToggleCollapse),
            )
        } else {
            // Keeps leaf names aligned with their expandable siblings; without it the tree stops
            // reading as a tree.
            Spacer(Modifier.width(MaterialTheme.dimens.icon.md))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
            ) {
                Text(
                    text = span.name,
                    style = MaterialTheme.typography.bodySmall,
                    // Unread is carried by contrast rather than weight — see WaterfallRow.
                    color = if (span.isViewed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.isCollapsed && row.descendantCount > 0) {
                    AlohomoraChip(
                        label = "+${row.descendantCount}",
                        uppercase = false,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isError) {
                    AlohomoraChip(
                        label = "error",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (row.isOrphan) {
                    AlohomoraChip(
                        label = "orphan",
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = "${span.kind.lowercase()} · ${formatDuration(span.durationNanos())}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .width(laneWidth)
                .height(MaterialTheme.dimens.margin.lg)
                .drawBehind {
                    // Lane guide, so an empty stretch still reads as "early in the trace" rather than
                    // as a missing bar.
                    drawLine(
                        color = laneColor,
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 1f,
                    )
                    val bar = window.barGeometry(
                        startNanos = span.startEpochNanos,
                        endNanos = span.endEpochNanos,
                        trackWidth = size.width,
                        minWidth = minBarWidth.toPx(),
                    )
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(bar.x, size.height * 0.25f),
                        size = Size(bar.width, size.height * 0.5f),
                        cornerRadius = CornerRadius(
                            cornerRadius.toPx(),
                            cornerRadius.toPx(),
                        ),
                    )
                },
        )
    }
}
