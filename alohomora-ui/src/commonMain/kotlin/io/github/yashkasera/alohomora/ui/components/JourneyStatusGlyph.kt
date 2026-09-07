package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import io.github.yashkasera.alohomora.common.journey.JourneyStatus
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.CircleHelp
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * A status badge for a [JourneyStatus], carrying M3 Expressive language: each state has its own
 * [MaterialShapes] silhouette (the `NeedsAttentionPager` precedent), and the fill/tint spring between
 * states on change. Emphasis is shape + colour + icon — never `FontWeight`, per the design system.
 *
 * A `null` status renders as the neutral "not yet run" state, so a row with no report reads sensibly.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun JourneyStatusGlyph(
    status: JourneyStatus?,
    modifier: Modifier = Modifier,
    size: Dp = MaterialTheme.dimens.icon.standard,
) {
    val colors = MaterialTheme.alohomoraColors
    val targetContainer: Color = when (status) {
        JourneyStatus.PASSED -> colors.successContainer
        JourneyStatus.FAILED -> colors.fatal.copy(alpha = FAILED_CONTAINER_ALPHA)
        JourneyStatus.NOT_EXERCISED, null -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val targetContent: Color = when (status) {
        JourneyStatus.PASSED -> colors.success
        JourneyStatus.FAILED -> colors.fatal
        JourneyStatus.NOT_EXERCISED, null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = when (status) {
        JourneyStatus.PASSED -> MaterialShapes.Cookie9Sided
        JourneyStatus.FAILED -> MaterialShapes.Circle
        JourneyStatus.NOT_EXERCISED, null -> MaterialShapes.Ghostish
    }
    val icon = when (status) {
        JourneyStatus.PASSED -> Icons.Check
        JourneyStatus.FAILED -> Icons.AlertTriangle
        JourneyStatus.NOT_EXERCISED, null -> Icons.CircleHelp
    }

    val container by animateColorAsState(targetContainer, label = "journeyGlyphContainer")
    val content by animateColorAsState(targetContent, label = "journeyGlyphContent")

    Box(
        modifier = modifier
            .size(size)
            .clip(shape.toShape())
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = status?.name,
            tint = content,
            modifier = Modifier.size(size * ICON_FRACTION),
        )
    }
}

private const val FAILED_CONTAINER_ALPHA = 0.12f
private const val ICON_FRACTION = 0.6f
