package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun TrafficItem(call: TrafficEntry, onClick: () -> Unit) {
    val containerColor = if (call.isSuccessful().not())
        MaterialTheme.colorScheme.errorContainer
    else if(call.isViewed) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surfaceContainerLowest

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.margin.xxl, vertical = MaterialTheme.dimens.margin.lg),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Text(
                    text = "${call.duration}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )

                val statusColor = when {
                    call.isSuccessful() -> MaterialTheme.colorScheme.onSurface // Design shows Black for 200 GET, Emerald for 201 etc. using Black for simplicity or custom logic
                    call.isViewed -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.error
                }
                // Override for 201 -> Emerald using theme color
                val finalStatusColor =
                    if (call.status == 201)
                        MaterialTheme.colorScheme.tertiary
                    else statusColor

                Text(
                    text = "${call.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = finalStatusColor,
                )
            }
        }

        Text(
            text = call.pathWithQuery(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = MaterialTheme.dimens.margin.xxxl),
        )

        Text(
            text = "host: ${call.host}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MethodBadge(method: String) {
    val isWrite = method in listOf("POST", "PUT", "PATCH", "DELETE")

    val backgroundColor =
        if (isWrite) MaterialTheme.colorScheme.inverseSurface
        else Color.Transparent

    val contentColor = if (isWrite) MaterialTheme.colorScheme.inverseOnSurface
    else MaterialTheme.colorScheme.onSurface

    AlohomoraChip(
        label = method,
        uppercase = true,
        containerColor = backgroundColor,
        contentColor = contentColor,
        borderStroke = BorderStroke(
            width = MaterialTheme.dimens.stroke.small,
            color = contentColor,
        ).takeIf { !isWrite },
    )
}
