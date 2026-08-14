package io.github.yashkasera.alohomora.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Lucide `waypoints` — four nodes around a hub, which reads as a call graph.
 *
 * Chosen over the alternatives for the Traces section specifically because it sits directly under
 * [Route] (Traffic) in the sidebar and has to be distinguishable from it at 24dp. `GitFork` collides
 * with [GitGraph] (Git History) the same way, and `ChartGantt` names the *view* rather than the
 * domain.
 */
val Icons.Waypoints: ImageVector
    get() {
        if (_waypoints != null) return _waypoints!!

        _waypoints = ImageVector.Builder(
            name = "waypoints",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.586f, 5.414f)
                lineToRelative(-5.172f, 5.172f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18.586f, 13.414f)
                lineToRelative(-5.172f, 5.172f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 12f)
                horizontalLineToRelative(12f)
            }
            // Four r=2 nodes: bottom, top, right, left. Each circle is two half-arcs, matching the
            // translation idiom in Route.kt — start at (cx - r, cy), sweep right, sweep back.
            spanNode(cx = 12f, cy = 20f)
            spanNode(cx = 12f, cy = 4f)
            spanNode(cx = 20f, cy = 12f)
            spanNode(cx = 4f, cy = 12f)
        }.build()

        return _waypoints!!
    }

private fun ImageVector.Builder.spanNode(cx: Float, cy: Float, r: Float = 2f) {
    path(
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 2f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    ) {
        moveTo(cx - r, cy)
        arcToRelative(r, r, 0f, true, false, 2 * r, 0f)
        arcToRelative(r, r, 0f, true, false, -2 * r, 0f)
    }
}

private var _waypoints: ImageVector? = null
