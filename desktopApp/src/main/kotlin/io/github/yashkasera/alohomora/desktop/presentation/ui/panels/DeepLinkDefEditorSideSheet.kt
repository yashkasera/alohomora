package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkDef
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkFieldErrors
import io.github.yashkasera.alohomora.common.deeplink.DeepLinkParam
import io.github.yashkasera.alohomora.common.deeplink.ParamType
import io.github.yashkasera.alohomora.common.deeplink.buildUri
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheetHeader
import io.github.yashkasera.alohomora.ui.components.AlohomoraAssistChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Typed editor for a [DeepLinkDef], plus a validated firing form. Fields, a params editor (name, type,
 * required, ENUM values), examples, live validation errors, and a build-and-fire section that renders a
 * typed input per param and only fires a well-formed URL ([buildUri]).
 *
 * Per-param description/example editing is deferred to raw-JSON editing; the model keeps those fields.
 */
@Composable
fun DeepLinkDefEditorSideSheet(
    draft: DeepLinkDef?,
    fieldErrors: DeepLinkFieldErrors,
    validationErrors: List<String>,
    onNameChange: (String) -> Unit,
    onModuleChange: (String) -> Unit,
    onFlowChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onUriTemplateChange: (String) -> Unit,
    onParamsChange: (List<DeepLinkParam>) -> Unit,
    onExamplesChange: (List<String>) -> Unit,
    onFire: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var shown by remember { mutableStateOf(draft) }
    if (draft != null) shown = draft
    val current = shown ?: return

    AlohomoraSideSheet(
        visible = draft != null,
        onDismiss = onDismiss,
        widthFraction = 0.4f,
        header = {
            AlohomoraSideSheetHeader(
                title = "Edit deep link",
                onClose = onDismiss,
                // Back returns to the deep link catalog, which stays open beneath this detail.
                onBack = onDismiss,
                actions = {
                    if (fieldErrors.isValid) {
                        AlohomoraChip(
                            label = "Saved",
                            containerColor = MaterialTheme.alohomoraColors.successContainer,
                            contentColor = MaterialTheme.alohomoraColors.success,
                        )
                    } else {
                        val n = fieldErrors.blockingCount
                        AlohomoraChip(
                            label = "$n issue${if (n == 1) "" else "s"}",
                            containerColor = MaterialTheme.alohomoraColors.warningContainer,
                            contentColor = MaterialTheme.alohomoraColors.warning,
                        )
                    }
                },
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
                    current.name,
                    onNameChange,
                    label = "Name *",
                    isError = fieldErrors.name != null,
                    supportingText = fieldErrors.name,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md)) {
                    AlohomoraTextField(
                        current.module,
                        onModuleChange,
                        label = "Module *",
                        isError = fieldErrors.module != null,
                        supportingText = fieldErrors.module,
                        modifier = Modifier.weight(1f),
                    )
                    AlohomoraTextField(
                        current.flow.orEmpty(),
                        { onFlowChange(it) },
                        label = "Flow (optional)",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                AlohomoraTextField(
                    current.description,
                    onDescriptionChange,
                    label = "Description (optional)",
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                AlohomoraTextField(
                    current.uriTemplate,
                    onUriTemplateChange,
                    label = "URI template *",
                    placeholder = "app://module/path/{param}",
                    isError = fieldErrors.uriTemplate != null,
                    supportingText = fieldErrors.uriTemplate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item { SectionLabel("Parameters") }
            itemsIndexed(current.params, key = { i, _ -> "param-$i" }) { index, param ->
                ParamRow(
                    param = param,
                    error = fieldErrors.params[index],
                    onChange = { updated ->
                        onParamsChange(current.params.toMutableList().also { it[index] = updated })
                    },
                    onRemove = { onParamsChange(current.params.filterIndexed { i, _ -> i != index }) },
                )
            }
            item {
                AlohomoraOutlinedButton(
                    text = "Add parameter",
                    size = AlohomoraButtonSize.SMALL,
                    onClick = {
                        onParamsChange(current.params + DeepLinkParam(name = "", type = ParamType.STRING))
                    },
                )
            }

            item { SectionLabel("Examples") }
            itemsIndexed(current.examples, key = { i, _ -> "example-$i" }) { index, example ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    AlohomoraTextField(
                        value = example,
                        onValueChange = { updated ->
                            onExamplesChange(current.examples.toMutableList().also { it[index] = updated })
                        },
                        modifier = Modifier.weight(1f),
                    )
                    AlohomoraIconButton(
                        onClick = { onExamplesChange(current.examples.filterIndexed { i, _ -> i != index }) },
                    ) {
                        Icon(
                            imageVector = Icons.Trash,
                            contentDescription = "Remove example",
                            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                        )
                    }
                }
            }
            item {
                AlohomoraTextButton(
                    text = "Add example",
                    onClick = { onExamplesChange(current.examples + "") },
                )
            }

            if (validationErrors.isNotEmpty()) {
                item { SectionLabel("Warnings") }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs)) {
                        validationErrors.forEach { warning ->
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { SectionLabel("Build and fire") }
            item { FireForm(current, onFire) }
        }
    }
}

@Composable
private fun ParamRow(
    param: DeepLinkParam,
    error: String?,
    onChange: (DeepLinkParam) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            AlohomoraTextField(
                value = param.name,
                onValueChange = { onChange(param.copy(name = it)) },
                placeholder = "name",
                isError = error != null,
                supportingText = error,
                modifier = Modifier.weight(1f),
            )
            AlohomoraAssistChip(
                label = param.type.name,
                onClick = { onChange(param.copy(type = ParamType.entries[(param.type.ordinal + 1) % ParamType.entries.size])) },
            )
            AlohomoraSwitch(checked = param.required, onCheckedChange = { onChange(param.copy(required = it)) })
            AlohomoraIconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Trash,
                    contentDescription = "Remove parameter",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                )
            }
        }
        if (param.type == ParamType.ENUM) {
            AlohomoraTextField(
                value = param.allowedValues.joinToString(", "),
                onValueChange = { text ->
                    onChange(param.copy(allowedValues = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }))
                },
                placeholder = "allowed values, comma separated",
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FireForm(def: DeepLinkDef, onFire: (String) -> Unit) {
    val args = remember(def.id) { mutableStateMapOf<String, String>() }
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
        def.params.forEach { param ->
            AlohomoraTextField(
                value = args[param.name].orEmpty(),
                onValueChange = { args[param.name] = it },
                label = paramLabel(param),
                placeholder = param.example ?: param.allowedValues.joinToString(" | ").ifEmpty { null },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val result = def.buildUri(args.toMap())
        result.url?.let { url ->
            AlohomoraCodeBlock(content = url, isScrollable = false)
            AlohomoraFilledButton(
                text = "Open on device",
                onClick = { onFire(url) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Play,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                },
            )
        }
        result.errors.forEach { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun paramLabel(param: DeepLinkParam): String {
    val required = if (param.required) "" else " (optional)"
    return "${param.name}: ${param.type.name.lowercase()}$required"
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
