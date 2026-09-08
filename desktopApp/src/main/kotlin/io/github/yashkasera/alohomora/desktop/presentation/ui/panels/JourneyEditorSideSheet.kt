package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import io.github.yashkasera.alohomora.common.journey.Assertion
import io.github.yashkasera.alohomora.common.journey.AssertOp
import io.github.yashkasera.alohomora.common.journey.JourneyDefinition
import io.github.yashkasera.alohomora.common.journey.JourneyReport
import io.github.yashkasera.alohomora.common.journey.JourneyStep
import io.github.yashkasera.alohomora.common.journey.RepeatPolicy
import io.github.yashkasera.alohomora.common.journey.StepOutcome
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheetHeader
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EventItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraAssistChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraSingleChoiceToggleGroup
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraToggleItem
import io.github.yashkasera.alohomora.ui.components.JourneyStatusGlyph
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.ChevronRight
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

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
    isNew: Boolean,
    report: JourneyReport?,
    recentEvents: List<Event>,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onToggleOrdered: (Boolean) -> Unit,
    onAddStepFromEvent: (String) -> Unit,
    onRemoveStep: (String) -> Unit,
    onStepSelectorChange: (String, Map<String, JsonElement>) -> Unit,
    onStepAssertionsChange: (String, List<Assertion>) -> Unit,
    onStepOnRepeatChange: (String, RepeatPolicy) -> Unit,
    onValidate: () -> Unit,
    onValidateLive: () -> Unit,
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
            AlohomoraSideSheetHeader(
                title = if (isNew) "New journey" else "Edit journey",
                onClose = onDismiss,
                // Back returns to the journey list, which stays open beneath this detail.
                onBack = onDismiss,
            )
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
            item { AddStepByNameRow(onAdd = onAddStepFromEvent) }
            if (current.steps.isEmpty()) {
                item {
                    Text(
                        text = "No steps yet. Add one from a recording below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(current.steps, key = { "step-${it.id}" }) { step ->
                    StepEditorRow(
                        step = step,
                        onRemove = { onRemoveStep(step.id) },
                        onSelectorChange = { onStepSelectorChange(step.id, it) },
                        onAssertionsChange = { onStepAssertionsChange(step.id, it) },
                        onOnRepeatChange = { onStepOnRepeatChange(step.id, it) },
                    )
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
                        enabled = current.steps.isNotEmpty(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Play,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                            )
                        },
                    )
                    AlohomoraOutlinedButton(
                        text = "Live",
                        onClick = onValidateLive,
                        enabled = current.steps.isNotEmpty(),
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
                items(report.steps, key = { "result-${it.step.id}" }) { result ->
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
                // Index-composite key: captured events can share an id (e.g. unpersisted), and a
                // LazyColumn key must be unique across the whole list.
                itemsIndexed(recentEvents, key = { index, event -> "event-$index-${event.id}" }) { _, event ->
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

/**
 * One step, with a collapsed "More" that reveals the identity/validation depth — selector (which
 * occurrence), assertions (what to check), and repeat policy. Progressive disclosure: collapsed by
 * default so a plain presence step stays one line, but available to every user (not developer-gated).
 */
@Composable
private fun StepEditorRow(
    step: JourneyStep,
    onRemove: () -> Unit,
    onSelectorChange: (Map<String, JsonElement>) -> Unit,
    onAssertionsChange: (List<Assertion>) -> Unit,
    onOnRepeatChange: (RepeatPolicy) -> Unit,
) {
    var expanded by remember(step.id) {
        mutableStateOf(step.selector.isNotEmpty() || step.assertions.isNotEmpty())
    }
    AlohomoraOutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                AlohomoraIconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.ChevronDown else Icons.ChevronRight,
                        contentDescription = if (expanded) "Collapse" else "More",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                }
                Text(
                    text = step.eventName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (step.selector.isNotEmpty()) AlohomoraChip(label = "selector")
                if (step.assertions.isNotEmpty()) AlohomoraChip(label = "${step.assertions.size} checks")
                AlohomoraIconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Trash,
                        contentDescription = "Remove step",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                }
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = MaterialTheme.dimens.margin.sm),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    SelectorEditor(step.selector, onSelectorChange)
                    AssertionEditor(step.assertions, onAssertionsChange)
                    Text(
                        text = "IF IT REPEATS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AlohomoraSingleChoiceToggleGroup(
                        items = REPEAT_ITEMS,
                        selectedId = step.onRepeat.name,
                        onSelectedIdChange = { onOnRepeatChange(RepeatPolicy.valueOf(it)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectorEditor(
    selector: Map<String, JsonElement>,
    onChange: (Map<String, JsonElement>) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    var value by remember { mutableStateOf("") }
    Text(
        text = "MATCH A SPECIFIC VALUE",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    selector.forEach { (k, v) ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            Text(
                text = "$k = ${scalarText(v)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AlohomoraIconButton(onClick = { onChange(selector - k) }) {
                Icon(
                    imageVector = Icons.X,
                    contentDescription = "Remove",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        AlohomoraTextField(
            value = key,
            onValueChange = { key = it },
            placeholder = "property",
            modifier = Modifier.weight(1f),
        )
        AlohomoraTextField(
            value = value,
            onValueChange = { value = it },
            placeholder = "value",
            modifier = Modifier.weight(1f),
        )
        AlohomoraTextButton(
            text = "Add",
            enabled = key.isNotBlank(),
            onClick = {
                onChange(selector + (key to parseScalar(value)))
                key = ""; value = ""
            },
        )
    }
}

@Composable
private fun AssertionEditor(
    assertions: List<Assertion>,
    onChange: (List<Assertion>) -> Unit,
) {
    var path by remember { mutableStateOf("") }
    var op by remember { mutableStateOf(AssertOp.EQ) }
    var value by remember { mutableStateOf("") }
    Text(
        text = "ADD A CHECK",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    assertions.forEachIndexed { index, assertion ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            Text(
                text = buildString {
                    append(assertion.path).append(' ').append(assertion.op.symbol())
                    assertion.value?.let { append(' ').append(scalarText(it)) }
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            AlohomoraIconButton(onClick = { onChange(assertions.filterIndexed { i, _ -> i != index }) }) {
                Icon(
                    imageVector = Icons.X,
                    contentDescription = "Remove",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        AlohomoraTextField(
            value = path,
            onValueChange = { path = it },
            placeholder = "property",
            modifier = Modifier.weight(1f),
        )
        var opMenuOpen by remember { mutableStateOf(false) }
        Box {
            AlohomoraAssistChip(
                label = op.symbol(),
                uppercase = false,
                onClick = { opMenuOpen = true },
            )
            AlohomoraDropdownMenu(
                expanded = opMenuOpen,
                onDismissRequest = { opMenuOpen = false },
            ) {
                AssertOp.entries.forEach { candidate ->
                    AlohomoraDropdownMenuItem(
                        text = { Text("${candidate.symbol()}  ${candidate.description()}") },
                        onClick = {
                            op = candidate
                            opMenuOpen = false
                        },
                    )
                }
            }
        }
        if (op != AssertOp.EXISTS) {
            AlohomoraTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = "value",
                modifier = Modifier.weight(1f),
            )
        }
        AlohomoraTextButton(
            text = "Add",
            enabled = path.isNotBlank(),
            onClick = {
                val assertion = Assertion(
                    path = path,
                    op = op,
                    value = if (op == AssertOp.EXISTS) null else parseScalar(value),
                )
                onChange(assertions + assertion)
                path = ""; value = ""
            },
        )
    }
}

/** A compact operator glyph for the chip: `=`, `≠`, `<`, `>`, and words for the non-arithmetic ops. */
private fun AssertOp.symbol(): String = when (this) {
    AssertOp.EQ -> "="
    AssertOp.NEQ -> "≠"
    AssertOp.LT -> "<"
    AssertOp.GT -> ">"
    AssertOp.EXISTS -> "exists"
    AssertOp.CONTAINS -> "contains"
}

/** The readable operator name for the dropdown row. */
private fun AssertOp.description(): String = when (this) {
    AssertOp.EQ -> "equals"
    AssertOp.NEQ -> "not equal"
    AssertOp.LT -> "less than"
    AssertOp.GT -> "greater than"
    AssertOp.EXISTS -> "exists"
    AssertOp.CONTAINS -> "contains"
}

/** Parses free-text input into the narrowest JSON scalar: bool, then number, else string. */
private fun parseScalar(input: String): JsonElement = when {
    input == "true" || input == "false" -> JsonPrimitive(input.toBoolean())
    input.toLongOrNull() != null -> JsonPrimitive(input.toLong())
    input.toDoubleOrNull() != null -> JsonPrimitive(input.toDouble())
    else -> JsonPrimitive(input)
}

private fun scalarText(element: JsonElement): String =
    (element as? JsonPrimitive)?.content ?: element.toString()

private val REPEAT_ITEMS = listOf(
    AlohomoraToggleItem(id = RepeatPolicy.IGNORE.name, label = "Ignore"),
    AlohomoraToggleItem(id = RepeatPolicy.FLAG.name, label = "Flag"),
    AlohomoraToggleItem(id = RepeatPolicy.FAIL.name, label = "Fail"),
)

/**
 * Add a step by typing an event name directly, for events not (yet) in the recording — so a journey
 * can assert an event *should* fire even before it has been seen. Complements the recording picker.
 */
@Composable
private fun AddStepByNameRow(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        AlohomoraTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Event name, e.g. checkout_started",
            modifier = Modifier.weight(1f),
        )
        AlohomoraTextButton(
            text = "Add step",
            enabled = name.isNotBlank(),
            onClick = {
                onAdd(name.trim())
                name = ""
            },
        )
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
