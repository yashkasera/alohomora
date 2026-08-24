package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraCheckbox
import io.github.yashkasera.alohomora.ui.components.MethodBadge
import io.github.yashkasera.alohomora.ui.components.rememberViewedStateColors
import io.github.yashkasera.alohomora.ui.icons.Check
import io.github.yashkasera.alohomora.ui.icons.CircleAlert
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.utils.drawDiagonalLabel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrafficItem(
    call: TrafficEntry,
    onClick: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onSelectionToggle: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val viewedColors = rememberViewedStateColors(call.isViewed)

    val baseModifier = if (call.isMocked()) {
        Modifier.fillMaxWidth().clipToBounds()
            .drawDiagonalLabel(
                text = "MOCKED",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
    } else {
        Modifier.fillMaxWidth()
    }

    AlohomoraCard(
        modifier = baseModifier
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = if (selectionMode) (onSelectionToggle ?: onClick) else onClick,
                onLongClick = onLongClick,
            ),
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = viewedColors.containerColor.value,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.md,
                )
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            if (selectionMode) {
                AlohomoraCheckbox(
                    checked = selected,
                    onCheckedChange = { onSelectionToggle?.invoke() },
                )
            }
            AlohomoraCard(
                modifier = Modifier.fillMaxHeight(),
                shape = MaterialTheme.shapes.medium,
                colors = AlohomoraCardDefaults.colors(
                    containerColor = if (call.isSuccessful())
                        MaterialTheme.alohomoraColors.successContainer
                    else
                        MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(MaterialTheme.dimens.margin.sm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(
                        MaterialTheme.dimens.margin.xs,
                        Alignment.CenterVertically,
                    ),
                ) {
                    Icon(
                        imageVector = if (call.isSuccessful())
                            Icons.Check
                        else Icons.CircleAlert,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                    Text(
                        text = call.status?.toString() ?: "???",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        MethodBadge(call.method.orEmpty())
                        Text(
                            text = DateUtils.format(call.time ?: 0, DateUtils.Format.HH_MM_SS_2MS),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (!call.isMocked()) {
                        Text(
                            text = "${call.duration}ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }

                Text(
                    text = call.pathWithQuery(),
                    style = MaterialTheme.typography.titleSmall,
                    color = viewedColors.titleColor.value,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = call.host.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
