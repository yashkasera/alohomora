package io.github.yashkasera.alohomora.ui.components.waterfall

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import io.github.yashkasera.alohomora.common.trace.TimeUnitScale
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.formatOffset
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlin.math.roundToInt

/**
 * The time axis above the waterfall rows.
 *
 * Carries the same left gutter as every row ([nameFraction]) so a tick lines up with the bar beneath
 * it. Labels are offsets from the trace start rather than wall-clock times: at tick density an
 * absolute timestamp is unreadable, and "did this start 200ms in" is the question being asked. The
 * absolute start belongs once, in the sheet header.
 */
@Composable
fun WaterfallAxis(
    window: TraceWindow,
    unit: TimeUnitScale,
    ticks: List<Long>,
    nameFraction: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.margin.xs),
    ) {
        Box(Modifier.fillMaxWidth(nameFraction))
        Box(Modifier.weight(1f)) {
            TickLabels(window = window, unit = unit, ticks = ticks)
        }
    }
}

/**
 * Lays each label out centred on its tick, clamped inside the track.
 *
 * A hand-rolled [Layout] rather than a [Row] with weights: labels sit at fractional positions along a
 * continuous axis, which no arrangement expresses, and the first and last must be nudged inward or
 * they hang off the edges and get clipped.
 */
@Composable
private fun TickLabels(window: TraceWindow, unit: TimeUnitScale, ticks: List<Long>) {
    Layout(
        content = {
            ticks.forEach { offset ->
                Text(
                    text = formatOffset(offset, unit),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        layout(constraints.maxWidth, height) {
            placeables.forEachIndexed { index, placeable ->
                val fraction = window.fractionOf(window.startNanos + ticks[index])
                val centred = (constraints.maxWidth * fraction).roundToInt() - placeable.width / 2
                val x = centred.coerceIn(0, (constraints.maxWidth - placeable.width).coerceAtLeast(0))
                placeable.place(IntOffset(x, 0))
            }
        }
    }
}

/** Vertical alignment shared by the axis and the rows, so the gutter split reads as one column. */
internal val WaterfallRowAlignment = Alignment.CenterVertically
