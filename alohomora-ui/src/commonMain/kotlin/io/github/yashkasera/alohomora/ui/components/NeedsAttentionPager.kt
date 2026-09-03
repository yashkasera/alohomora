package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.AttentionItem
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

private val AttentionCardHeight = 104.dp
private val AttentionIconBadgeSize = 40.dp

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
            AlohomoraChip(
                label = items.size.toString(),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }

        Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            beyondViewportPageCount = 1,
            // Peek layout: the current card starts flush-left and the next one peeks in from the
            // end, which advertises the swipe without the dead side margins of a center snap.
            contentPadding = if (items.size > 1) {
                PaddingValues(end = MaterialTheme.dimens.margin.huge)
            } else {
                PaddingValues()
            },
            pageSpacing = MaterialTheme.dimens.margin.md,
            snapPosition = SnapPosition.Start,
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
        if (items.size > 1) {
            Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
            AlohomoraPageIndicator(
                pageCount = items.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ErrorAttentionCard(
    error: Error,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AttentionCardHeight)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.errorContainer)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.margin.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Box(
            modifier = Modifier
                .size(AttentionIconBadgeSize)
                .clip(MaterialShapes.Cookie9Sided.toShape())
                .background(MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AlertTriangle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
        ) {
            Text(
                text = error.exceptionTypeName(),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onErrorContainer,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                Text(
                    text = error.place ?: "Unknown location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = DateUtils.format(error.time, DateUtils.Format.HH_MM_SS),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TrafficAttentionCard(
    entry: TrafficEntry,
    onClick: () -> Unit,
) {
    val warning = MaterialTheme.alohomoraColors.warning
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(AttentionCardHeight)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.alohomoraColors.warningContainer)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(MaterialTheme.dimens.margin.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Box(
            modifier = Modifier
                .size(AttentionIconBadgeSize)
                .clip(MaterialShapes.Ghostish.toShape())
                .background(warning.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.CircleAlert,
                contentDescription = null,
                tint = warning,
                modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
        ) {
            Text(
                text = entry.pathWithQuery(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                entry.method?.let { method ->
                    MethodBadge(method = method)
                }
                AlohomoraChip(
                    label = entry.status?.toString() ?: "???",
                    containerColor = Color.Transparent,
                    contentColor = warning,
                    border = BorderStroke(MaterialTheme.dimens.stroke.small, warning),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = DateUtils.format(entry.time ?: 0L, DateUtils.Format.HH_MM_SS),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun NeedsAttentionPagerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xl)) {
                NeedsAttentionPager(
                    items = listOf(
                        AttentionItem.UnviewedError(
                            Error(
                                id = 1,
                                place = "CheckoutViewModel.submit",
                                reason = "IllegalStateException: Cart is empty",
                                time = 1_726_000_000_000,
                            ),
                        ),
                        AttentionItem.FailedTraffic(
                            TrafficEntry(
                                id = "preview-traffic",
                                status = 500,
                                method = "GET",
                                host = "api.example.com",
                                path = "/v1/checkout",
                                query = "retry=1",
                                time = 1_726_000_030_000,
                            ),
                        ),
                    ),
                    onErrorClick = {},
                    onTrafficClick = {},
                )
            }
        }
    }
}
