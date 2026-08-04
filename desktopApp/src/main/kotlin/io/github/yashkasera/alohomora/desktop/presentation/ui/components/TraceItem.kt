package io.github.yashkasera.alohomora.desktop.presentation.ui.components

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Span
import io.github.yashkasera.alohomora.common.trace.TraceSummary
import io.github.yashkasera.alohomora.common.trace.formatDuration
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.theme.muted
import io.github.yashkasera.alohomora.ui.theme.mutedContainer

/**
 * One trace as a list row. Two dense lines, mirroring [TrafficItem].
 *
 * The container-colour ladder is deliberately identical to [TrafficItem]'s: a failed record reads red,
 * an opened one reads dimmed, and everything else reads plain, in every panel.
 */
@Composable
fun TraceItem(trace: TraceSummary, onClick: () -> Unit) {
    val containerColor = when {
        trace.hasError -> MaterialTheme.colorScheme.errorContainer
        trace.isViewed -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.background
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.lg,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                Text(
                    // "pending" rather than "unknown": a nameless trace is in flight, not broken.
                    // See TraceSummary.rootSpanName.
                    text = trace.rootSpanName ?: "(root pending)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (trace.isViewed) FontWeight.Normal else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (trace.hasError) {
                    AlohomoraChip(
                        label = "error",
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
                if (!trace.isComplete) {
                    AlohomoraChip(
                        label = "partial",
                        containerColor = MaterialTheme.colorScheme.mutedContainer,
                        contentColor = MaterialTheme.colorScheme.muted,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Text(
                    text = formatDuration(trace.durationNanos),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${trace.spanCount} ${if (trace.spanCount == 1) "span" else "spans"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.muted,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DateUtils.format(trace.startMillis, DateUtils.Format.HH_MM_SS_2MS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.muted,
            )
            trace.scopeName?.let { scope ->
                Text(
                    text = scope,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Text(
                // A prefix, not the whole 32 hex chars: enough to correlate with a backend trace or a
                // log line, short enough not to crowd the row.
                text = trace.traceId.take(TRACE_ID_PREFIX_LENGTH),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.muted,
            )
        }
    }
}

/** Enough of a [Span.traceId] to correlate by eye; the sheet header offers the full id to copy. */
private const val TRACE_ID_PREFIX_LENGTH = 8
