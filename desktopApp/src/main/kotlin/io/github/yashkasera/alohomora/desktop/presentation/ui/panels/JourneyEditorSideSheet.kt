package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.common.journey.StepOutcome
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EventItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.JourneyStatusGlyph
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Detail editor (~40% width), mirroring `EditMockRuleSideSheet`. Progressive disclosure that is
 * available to *every* user (not gated on developer mode): a plain presence checklist by default, an
 * Enforce-order switch, and build-from-recording that promotes a captured event to a step by reuse of
 * the desktop [EventItem].
 *
 * Stateless over [draft]; a `null` draft hides the sheet. Selector / assertion / onRepeat authoring is
 * a follow-up slice (the per-step "More" expander).
 */
@Composable
fun JourneyEditorSideSheet(
    draft: JourneyDefinition?,
    report: JourneyReport?,
    recentEvents: List<Event>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onToggleOrdered: (Boolean) -> Unit,
    onAddStepFromEvent: (String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onValidate: () -> Unit,
    onDismiss: () -> Unit,
) {
    // Keep the last non-null draft so the exit slide has something to render after draft goes null.
    var shown by remember { mutableStateOf(draft) }
    if (draft != null) shown = draft
    val current = shown ?: return

    AlohomoraSideSheet(
        visible = draft != null,
        onDismiss = onDismiss,
        widthFraction = 0.4f,
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
                Text(
                    text = "Edit journey",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                AlohomoraIconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.X,
                        contentDescription = "Close",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                    )
                }
            }
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.dimens.margin.xxl,
                vertical = MaterialTheme.dimens.margin.md,
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            item {
                AlohomoraTextField(
                    value = current.name,
                    onValueChange = onNameChange,
                    label = "Name",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                AlohomoraTextField(
                    value = current.description,
                    onValueChange = onDescriptionChange,
                    label = "Description (optional)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enforce order", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "Steps must fire in sequence, not just be present.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AlohomoraSwitch(checked = current.ordered, onCheckedChange = onToggleOrdered)
                }
            }

            item { SectionLabel("Steps") }
            if (current.steps.isEmpty()) {
                item {
                    Text(
                        text = "No steps yet. Add one from a recording below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(current.steps, key = { it.id }) { step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        Text(
                            text = step.eventName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (step.selector.isNotEmpty()) AlohomoraChip(label = "selector")
                        if (step.assertions.isNotEmpty()) AlohomoraChip(label = "${step.assertions.size} checks")
                        AlohomoraIconButton(onClick = { onRemoveStep(step.id) }) {
                            Icon(
                                imageVector = Icons.Trash,
                                contentDescription = "Remove step",
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MaterialTheme.dimens.margin.sm),
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AlohomoraFilledButton(
                        text = "Validate",
                        onClick = onValidate,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            )
                        },
                    )
                    if (report != null) {
                        JourneyStatusGlyph(status = report.status)
                        Text(
                            text = report.status.name.replace('_', ' '),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            if (report != null) {
                items(report.steps, key = { it.step.id }) { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = result.step.eventName,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AlohomoraChip(
                            label = result.outcome.name.replace('_', ' '),
                            containerColor = outcomeColor(result.outcome),
                        )
                    }
                }
            }

            item { SectionLabel("Add from recording") }
            if (recentEvents.isEmpty()) {
                item {
                    Text(
                        text = "No captured events. Record a session to build steps from it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(recentEvents, key = { it.id }) { event ->
                    EventItem(
                        event = event,
                        showProperties = false,
                        onClick = { onAddStepFromEvent(event.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
    )
}

@Composable
private fun outcomeColor(outcome: StepOutcome) = when (outcome) {
    StepOutcome.MATCHED -> MaterialTheme.colorScheme.surfaceContainerHigh
    else -> MaterialTheme.colorScheme.errorContainer
}
