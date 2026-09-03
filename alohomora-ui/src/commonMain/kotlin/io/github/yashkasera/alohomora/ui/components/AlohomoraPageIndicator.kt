package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

object AlohomoraPageIndicatorDefaults {
    val dotSize: Dp = 6.dp
    val activeWidth: Dp = 20.dp
}

/**
 * Width-morphing page dots: the active page stretches into a pill, the rest stay dots. Springs are
 * one-shot per page change, so the indicator is idle at rest and safe under `waitForIdle()`.
 * Renders nothing for a single page — a lone pill is noise, not navigation.
 */
@Composable
fun AlohomoraPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.onSurface,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    if (pageCount <= 1) return

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val width by animateDpAsState(
                targetValue = if (selected) {
                    AlohomoraPageIndicatorDefaults.activeWidth
                } else {
                    AlohomoraPageIndicatorDefaults.dotSize
                },
                animationSpec = spring(
                    dampingRatio = 0.65f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            val color by animateColorAsState(
                targetValue = if (selected) activeColor else inactiveColor,
                animationSpec = spring(),
            )
            Box(
                modifier = Modifier
                    .height(AlohomoraPageIndicatorDefaults.dotSize)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Preview
@Composable
private fun AlohomoraPageIndicatorPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                AlohomoraPageIndicator(pageCount = 5, currentPage = 2)
                AlohomoraPageIndicator(pageCount = 3, currentPage = 0)
                // pageCount = 1 renders nothing
                AlohomoraPageIndicator(pageCount = 1, currentPage = 0)
            }
        }
    }
}
