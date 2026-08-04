package io.github.yashkasera.alohomora.ui.components.waterfall

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.trace.TraceWindow
import io.github.yashkasera.alohomora.common.trace.barGeometry
import kotlin.math.roundToInt

/**
 * Draws the vertical gridlines at each axis tick.
 *
 * Drawn per row from the same tick list the axis uses, rather than once behind the whole list. One
 * source of truth for the x positions means a gridline cannot drift from its label, and the lines
 * scroll with the rows for free.
 */
internal fun DrawScope.drawGridlines(window: TraceWindow, ticks: List<Long>, color: Color) {
    if (ticks.isEmpty()) return
    ticks.forEach { offset ->
        val x = size.width * window.fractionOf(window.startNanos + offset)
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1f,
        )
    }
}

/**
 * Draws one duration bar.
 *
 * The geometry — including the minimum-width rule that keeps instantaneous and sub-pixel spans
 * visible — lives in `TraceWindow.barGeometry` in `:alohomora-common`, not here. A draw scope is
 * unreachable from a test, and that clamping is the most consequential arithmetic in the waterfall;
 * `TraceTimeScaleTest` covers it directly. This function is only the paint call.
 */
internal fun DrawScope.drawBar(
    startNanos: Long,
    endNanos: Long,
    window: TraceWindow,
    color: Color,
    minWidthPx: Float,
    cornerRadiusPx: Float,
    heightFraction: Float,
) {
    val bar = window.barGeometry(
        startNanos = startNanos,
        endNanos = endNanos,
        trackWidth = size.width,
        minWidth = minWidthPx,
    )
    val barHeight = size.height * heightFraction
    drawRoundRect(
        color = color,
        topLeft = Offset(bar.x, (size.height - barHeight) / 2f),
        size = Size(bar.width, barHeight),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
    )
}

/**
 * Draws a tick for each span event, full row height so it reads against the bar.
 *
 * Deduplicated by pixel bucket: a retry loop can emit 50 events inside one bar, and drawing each would
 * paint a solid block that hides both the events and the bar. Bucketing also caps the draw cost per
 * row regardless of how event-happy the instrumentation is.
 *
 * Not hit-testable — a 1px target cannot be clicked reliably. Events are inspected in the selected
 * span's Events tab, which is also why nothing here is the only place an event's data appears.
 */
internal fun DrawScope.drawEventTicks(
    span: Span,
    window: TraceWindow,
    color: Color,
    widthPx: Float,
) {
    if (span.events.isEmpty()) return
    val drawn = mutableSetOf<Int>()
    span.events.forEach { event ->
        val x = size.width * window.fractionOf(event.epochNanos)
        val bucket = (x / widthPx.coerceAtLeast(1f)).roundToInt()
        if (!drawn.add(bucket)) return@forEach
        drawLine(
            color = color,
            start = Offset(x, size.height * 0.15f),
            end = Offset(x, size.height * 0.85f),
            strokeWidth = widthPx.coerceAtLeast(1f),
        )
    }
}
