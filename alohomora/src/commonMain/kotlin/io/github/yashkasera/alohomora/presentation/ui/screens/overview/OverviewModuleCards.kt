package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.icons.Activity
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

// Rest/pressed corner radii mirror MaterialTheme.shapes.large (16) and extraLarge (28) — the press
// morphs the card one step up the shape scale.
private val StandardRestCorner = 16.dp
private val FeaturedRestCorner = 28.dp
private val StandardPressedCorner = 28.dp
private val FeaturedPressedCorner = 16.dp
private const val PressedScale = 0.97f

@Composable
internal fun ModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
    count: Long? = null,
) {
    if (overviewModule.isInverse) {
        FeaturedModuleCard(overviewModule, onNavigate, modifier, count)
    } else {
        StandardModuleCard(overviewModule, onNavigate, modifier, count)
    }
}

/** One-shot spring press morph: corner radius shifts one step and the card dips to 97% scale. */
@Composable
private fun pressMorph(
    interactionSource: MutableInteractionSource,
    restCorner: Dp,
    pressedCorner: Dp,
): Pair<Dp, Float> {
    val pressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) pressedCorner else restCorner,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
    )
    val scale by animateFloatAsState(
        targetValue = if (pressed) PressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
    )
    return corner to scale
}

@Composable
private fun CountBadge(
    count: Long,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = if (count > 999) "999+" else count.toString(),
        transitionSpec = {
            (
                scaleIn(spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)) +
                    fadeIn()
                ) togetherWith fadeOut()
        },
        modifier = modifier,
    ) { label ->
        AlohomoraChip(
            label = label,
            containerColor = tint.copy(alpha = 0.12f),
            contentColor = tint,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FeaturedModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
    count: Long? = null,
) {
    val isEvents = overviewModule.route == Routes.Events
    val containerColor = if (isEvents)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isEvents)
        MaterialTheme.colorScheme.onTertiaryContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    val interactionSource = remember { MutableInteractionSource() }
    val (corner, scale) = pressMorph(interactionSource, FeaturedRestCorner, FeaturedPressedCorner)

    AlohomoraCard(
        onClick = { onNavigate(overviewModule.route) },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(corner),
        colors = AlohomoraCardDefaults.colors(containerColor = containerColor),
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.lg,
                        vertical = MaterialTheme.dimens.margin.xxl,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimens.margin.huge)
                        .background(
                            contentColor.copy(alpha = 0.12f),
                            MaterialShapes.Cookie6Sided.toShape(),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = overviewModule.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                    )
                }
                Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
                Text(
                    text = overviewModule.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = contentColor,
                )
                Text(
                    text = overviewModule.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.7f),
                )
            }
            if (count != null && count > 0) {
                CountBadge(
                    count = count,
                    tint = contentColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.dimens.margin.md),
                )
            }
        }
    }
}

@Composable
private fun StandardModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
    count: Long? = null,
) {
    val iconTint = when (overviewModule.route) {
        Routes.Error -> MaterialTheme.colorScheme.error
        Routes.Traces, Routes.FeatureFlags, Routes.GitHistory -> MaterialTheme.colorScheme.tertiary
        Routes.Database, Routes.Config -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val (corner, scale) = pressMorph(interactionSource, StandardRestCorner, StandardPressedCorner)

    AlohomoraCard(
        onClick = { onNavigate(overviewModule.route) },
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(corner),
        colors = AlohomoraCardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        interactionSource = interactionSource,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(MaterialTheme.dimens.margin.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(MaterialTheme.dimens.icon.xl)
                        .background(iconTint.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = overviewModule.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }
                Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
                Text(
                    text = overviewModule.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = overviewModule.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (count != null && count > 0) {
                CountBadge(
                    count = count,
                    tint = iconTint,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(MaterialTheme.dimens.margin.sm),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ModuleCardsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                ModuleCard(
                    overviewModule = OverviewModule(
                        title = "Traffic",
                        subtitle = "LIVE STREAM",
                        icon = Icons.Route,
                        isInverse = true,
                        route = Routes.Traffic,
                    ),
                    onNavigate = {},
                    count = 128,
                )
                ModuleCard(
                    overviewModule = OverviewModule(
                        title = "Events",
                        subtitle = "SYSTEM TRIGGERS",
                        icon = Icons.Activity,
                        isInverse = true,
                        route = Routes.Events,
                    ),
                    onNavigate = {},
                    // A zero count hides the badge.
                    count = 0,
                )
            }
        }
    }
}

@Preview
@Composable
private fun StandardModuleCardsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                items(
                    items = builtInModules.filter { !it.isInverse },
                    key = { it.gridKey },
                ) { module ->
                    ModuleCard(
                        overviewModule = module,
                        onNavigate = {},
                        count = when (module.route) {
                            Routes.Error -> 4L
                            Routes.Database -> null
                            else -> null
                        },
                    )
                }
            }
        }
    }
}
