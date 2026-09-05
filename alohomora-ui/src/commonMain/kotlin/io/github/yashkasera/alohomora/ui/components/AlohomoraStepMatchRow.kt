package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * One expected-step row in a journey alignment view. The leading marker springs to a filled success
 * dot when the step is [matched]; the pending [isCurrent] step is emphasised with an accent marker and
 * a subtle spring scale so the cursor advance reads as motion. Emphasis is colour + scale + icon —
 * never `FontWeight`.
 */
@Composable
fun AlohomoraStepMatchRow(
    label: String,
    matched: Boolean,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    detail: String? = null,
) {
    val colors = MaterialTheme.alohomoraColors
    val targetMarker: Color = when {
        matched -> colors.success
        isCurrent -> colors.accent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val marker by animateColorAsState(targetMarker, label = "stepMarker")
    val scale by animateFloatAsState(
        targetValue = if (isCurrent && !matched) CURRENT_SCALE else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "stepMarkerScale",
    )
    val labelColor = when {
        matched -> MaterialTheme.colorScheme.onSurface
        isCurrent -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Box(
            modifier = Modifier
                .scale(scale)
                .size(MaterialTheme.dimens.icon.lg)
                .clip(CircleShape)
                .background(marker),
            contentAlignment = Alignment.Center,
        ) {
            if (matched) {
                Icon(
                    imageVector = Icons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = MaterialTheme.dimens.margin.sm),
            )
        }
    }
}

private const val CURRENT_SCALE = 1.15f
