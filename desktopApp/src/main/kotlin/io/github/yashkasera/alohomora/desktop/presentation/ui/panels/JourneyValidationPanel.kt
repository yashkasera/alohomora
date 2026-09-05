package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.common.journey.StepOutcome
import io.github.yashkasera.alohomora.common.journey.TimelineTag
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraStepMatchRow
import io.github.yashkasera.alohomora.ui.components.JourneyStatusGlyph
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Full-width live validation view: expected steps on the left aligned against the live event stream on
 * the right, with the status glyph shape-morphing and the cursor springing forward as steps match. Wide
 * because the alignment needs the horizontal room.
 *
 * Match *lines* between the two columns are a later refinement; today the columns align by colour and
 * the per-step spring marker ([AlohomoraStepMatchRow]).
 */
@Composable
fun JourneyValidationPanel(
    visible: Boolean,
    journeyName: String,
    report: JourneyReport?,
    onClose: () -> Unit,
) {
    AlohomoraSideSheet(
        visible = visible,
        onDismiss = onClose,
        widthFraction = 0.9f,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.lg,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                JourneyStatusGlyph(status = report?.status)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = journeyName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = report?.status?.name?.replace('_', ' ') ?: "Waiting for events",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AlohomoraIconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.X,
                        contentDescription = "Close",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }
            }
        },
    ) {
        if (report == null || report.steps.isEmpty()) {
            Text(
                text = "No steps to validate yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl),
            )
            return@AlohomoraSideSheet
        }

        val currentIndex = report.steps.indexOfFirst { it.outcome != StepOutcome.MATCHED }

        Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            // Left: expected steps.
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ColumnLabel("Expected")
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                ) {
                    itemsIndexed(report.steps, key = { _, r -> r.step.id }) { index, result ->
                        AlohomoraStepMatchRow(
                            label = result.step.eventName,
                            matched = result.outcome == StepOutcome.MATCHED,
                            isCurrent = index == currentIndex,
                            detail = if (result.outcome == StepOutcome.MATCHED) {
                                result.matchedEventTime?.let {
                                    DateUtils.format(it, DateUtils.Format.HH_MM_SS)
                                }
                            } else {
                                result.outcome.name.replace('_', ' ')
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .width(MaterialTheme.dimens.stroke.small)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )

            // Right: the live event stream, tagged.
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                ColumnLabel("Live stream")
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = MaterialTheme.dimens.margin.xxl,
                        vertical = MaterialTheme.dimens.margin.md,
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    items(report.timeline.size) { i ->
                        val entry = report.timeline[i]
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        ) {
                            Text(
                                text = entry.event.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = tagColor(entry.tag),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            AlohomoraChip(label = entry.tag.name)
                            Text(
                                text = DateUtils.format(entry.event.time, DateUtils.Format.HH_MM_SS),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            horizontal = MaterialTheme.dimens.margin.xxl,
            vertical = MaterialTheme.dimens.margin.md,
        ),
    )
}

@Composable
private fun tagColor(tag: TimelineTag): Color = when (tag) {
    TimelineTag.MATCHED -> MaterialTheme.alohomoraColors.success
    TimelineTag.REPEAT -> MaterialTheme.alohomoraColors.warning
    TimelineTag.UNEXPECTED -> MaterialTheme.alohomoraColors.fatal
    TimelineTag.NOISE -> MaterialTheme.colorScheme.onSurfaceVariant
}
