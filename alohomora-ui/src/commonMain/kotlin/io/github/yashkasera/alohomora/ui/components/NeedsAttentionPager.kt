package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.AttentionItem
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun NeedsAttentionPager(
    items: List<AttentionItem>,
    onErrorClick: (Error) -> Unit,
    onTrafficClick: (TrafficEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            Text(
                "NEEDS ATTENTION",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Badge {
                Text(
                    items.size.toString(),
                )
            }
        }

        Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
        BoxWithConstraints {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
                beyondViewportPageCount = 1,
                pageSize = PageSize.Fixed(72.dp),
                pageSpacing = MaterialTheme.dimens.margin.md,
                snapPosition = SnapPosition.Center,
            ) { page ->
                when (val item = items[page]) {
                    is AttentionItem.UnviewedError -> ErrorAttentionCard(
                        error = item.error,
                        onClick = { onErrorClick(item.error) },
                    )

                    is AttentionItem.FailedTraffic -> TrafficAttentionCard(
                        entry = item.entry,
                        onClick = { onTrafficClick(item.entry) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorAttentionCard(
    error: Error,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.medium)
            .fillMaxHeight()
            .height(72.dp)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.colorScheme.errorContainer.copy(0.5f),
                    ),
                ),
            )
            .clickable(onClick = onClick)

            .padding(MaterialTheme.dimens.margin.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Icon(
            imageVector = Icons.AlertTriangle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = error.exceptionTypeName(),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onErrorContainer,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = error.place ?: "Unknown location",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                modifier = Modifier.align(Alignment.End),
                textAlign = TextAlign.End,
                text = DateUtils.format(error.time, DateUtils.Format.HH_MM_SS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun TrafficAttentionCard(
    entry: TrafficEntry,
    onClick: () -> Unit,
) {
    val warningColor = MaterialTheme.alohomoraColors.warning
    Row(
        modifier = Modifier
            .height(72.dp)
            .clip(MaterialTheme.shapes.medium)
            .fillMaxHeight()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        warningColor,
                        warningColor.copy(alpha = 0.8f),
                    ),
                ),
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .padding(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Column(
                modifier = Modifier.fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimens.margin.xs,
                    Alignment.CenterVertically,
                ),
            ) {
                Icon(
                    imageVector = Icons.CircleAlert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
                Text(
                    text = entry.status?.toString() ?: "???",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    MaterialTheme.dimens.margin.xs,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    entry.method?.let { method ->
                        AlohomoraChip(label = method, uppercase = true)
                    }
                    Text(
                        textAlign = TextAlign.End,
                        text = DateUtils.format(entry.time ?: 0L, DateUtils.Format.HH_MM_SS),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }

                Text(
                    text = entry.pathWithQuery(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
