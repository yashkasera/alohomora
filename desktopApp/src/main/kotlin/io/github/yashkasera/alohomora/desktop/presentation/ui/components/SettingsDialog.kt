package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import androidx.compose.material3.MaterialTheme
import io.github.yashkasera.alohomora.ui.components.AlohomoraRadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import io.github.yashkasera.alohomora.desktop.app.ThemeMode
import io.github.yashkasera.alohomora.desktop.app.isShortcutModifier
import io.github.yashkasera.alohomora.desktop.mcp.AlohomoraMcpServer
import io.github.yashkasera.alohomora.desktop.mcp.McpClient
import io.github.yashkasera.alohomora.desktop.mcp.McpClientConfig
import io.github.yashkasera.alohomora.desktop.mcp.McpServerStatus
import androidx.compose.material3.Icon
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraSingleChoiceToggleGroup
import io.github.yashkasera.alohomora.ui.components.AlohomoraSwitch
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraToggleItem
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.AlohomoraColorTheme
import io.github.yashkasera.alohomora.ui.theme.AlohomoraThemes
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun SettingsDialog(
    isDark: Boolean,
    themeId: String,
    themeMode: ThemeMode,
    onThemeIdChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onClearTrustTokens: () -> Unit,
    onClearMutedEvents: () -> Unit,
    onResetPreferences: () -> Unit,
    mcpEnabled: Boolean,
    mcpPort: Int,
    mcpStatus: McpServerStatus,
    mcpWriteEnabled: Boolean,
    onMcpEnabledChange: (Boolean) -> Unit,
    onMcpPortChange: (Int) -> Unit,
    onMcpWriteEnabledChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState(width = 780.dp, height = 620.dp)
    DialogWindow(
        title = "Preferences",
        state = state,
        onCloseRequest = onDismiss,
        resizable = false,
    ) {
        AppTheme(initialIsDark = isDark, themeId = themeId) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when {
                            event.key == Key.Escape -> {
                                onDismiss(); true
                            }

                            event.key == Key.W && event.isShortcutModifier() -> {
                                onDismiss(); true
                            }

                            else -> false
                        }
                    },
            ) {
                var section by remember { mutableStateOf(SettingsSection.APPEARANCE) }

                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(184.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(MaterialTheme.dimens.margin.md),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                    ) {
                        SettingsSection.entries.forEach { item ->
                            SettingsNavItem(
                                label = item.label,
                                selected = item == section,
                                onClick = { section = item },
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.dimens.margin.xl),
                    ) {
                        SectionHeader(section.label)
                        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                        when (section) {
                            SettingsSection.APPEARANCE -> AppearanceSection(
                                themeId = themeId,
                                isDark = isDark,
                                themeMode = themeMode,
                                onThemeIdChange = onThemeIdChange,
                                onThemeModeChange = onThemeModeChange,
                            )

                            SettingsSection.MCP -> McpServerSection(
                                enabled = mcpEnabled,
                                port = mcpPort,
                                status = mcpStatus,
                                writeEnabled = mcpWriteEnabled,
                                onEnabledChange = onMcpEnabledChange,
                                onPortChange = onMcpPortChange,
                                onWriteEnabledChange = onMcpWriteEnabledChange,
                            )

                            SettingsSection.DATA -> DataSection(
                                onClearTrustTokens = onClearTrustTokens,
                                onClearMutedEvents = onClearMutedEvents,
                                onResetPreferences = onResetPreferences,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun McpServerSection(
    enabled: Boolean,
    port: Int,
    status: McpServerStatus,
    writeEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onPortChange: (Int) -> Unit,
    onWriteEnabledChange: (Boolean) -> Unit,
) {
    Text(
        text = "Serve data captured from your connected devices to your own AI agent (Claude Code, " +
            "Cursor) over an MCP endpoint on loopback. One server for the whole app. Reads only, " +
            "unless you turn on write tools below. Off by default.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Enable MCP server", style = MaterialTheme.typography.bodyMedium)
        AlohomoraSwitch(checked = enabled, onCheckedChange = onEnabledChange)
    }

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    // Local text so a half-typed port doesn't fight the parsed value; only a valid port is pushed up.
    var portText by remember(port) { mutableStateOf(port.toString()) }
    AlohomoraTextField(
        value = portText,
        onValueChange = { raw ->
            val digits = raw.filter(Char::isDigit).take(5)
            portText = digits
            digits.toIntOrNull()?.let { if (it in 1..65535) onPortChange(it) }
        },
        label = "Port",
        singleLine = true,
        enabled = !enabled,
        modifier = Modifier.width(160.dp),
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    LabeledCopyBlock(label = "Endpoint", content = AlohomoraMcpServer.endpointUrl(port))

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

    val (statusText, statusColor) = when (status) {
        McpServerStatus.Stopped -> "Stopped" to MaterialTheme.colorScheme.onSurfaceVariant
        is McpServerStatus.Listening -> {
            val clients = when (status.connectedClients) {
                0 -> "no agent connected"
                1 -> "1 agent connected"
                else -> "${status.connectedClients} agents connected"
            }
            "Listening on 127.0.0.1:${status.port} · $clients" to MaterialTheme.colorScheme.primary
        }

        is McpServerStatus.Error -> "Error: ${status.message}" to MaterialTheme.colorScheme.error
    }
    Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = statusColor)

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xl))
    AlohomoraHorizontalDivider()
    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xl))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "Allow write tools", style = MaterialTheme.typography.bodyMedium)
        AlohomoraSwitch(checked = writeEnabled, onCheckedChange = onWriteEnabledChange, enabled = enabled)
    }

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))

    Text(
        text = "Lets the agent replay requests, set mock rules and throttling, and clear captured data. " +
            "Clearing data always asks you to confirm here first.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xl))

    Text(
        text = "Connect your agent",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

    var selectedClient by remember { mutableStateOf(McpClient.CLAUDE_CODE) }

    AlohomoraSingleChoiceToggleGroup(
        items = McpClient.entries.map { AlohomoraToggleItem(id = it.name, label = it.label) },
        selectedId = selectedClient.name,
        onSelectedIdChange = { selectedClient = McpClient.valueOf(it) },
    )

    McpClientConfig.command(selectedClient, port)?.let { command ->
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
        LabeledCopyBlock(label = "Command", content = command)
    }

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

    LabeledCopyBlock(label = "Config", content = McpClientConfig.config(selectedClient, port))

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

    Text(
        text = McpClientConfig.hint(selectedClient),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A right-aligned copy affordance over a code block, so the action reads as a button, not a stray label. */
@Composable
private fun LabeledCopyBlock(label: String, content: String) {
    val clipboard = LocalClipboardManager.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AlohomoraIconButton(onClick = { clipboard.setText(AnnotatedString(content)) }) {
            Icon(
                imageVector = Icons.Copy,
                contentDescription = "Copy $label",
                modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
    AlohomoraCodeBlock(content = content, isScrollable = false)
}

private enum class SettingsSection(val label: String) {
    APPEARANCE("Appearance"),
    MCP("MCP server"),
    DATA("Data"),
}

@Composable
private fun SettingsNavItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val background =
        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val contentColor =
        if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = MaterialTheme.dimens.margin.md, vertical = MaterialTheme.dimens.margin.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = contentColor)
    }
}

@Composable
private fun AppearanceSection(
    themeId: String,
    isDark: Boolean,
    themeMode: ThemeMode,
    onThemeIdChange: (String) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
) {
    Text(
        text = "Theme",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

    ThemePicker(selectedId = themeId, isDark = isDark, onSelect = onThemeIdChange)

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    Text(
        text = "Mode",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

    ThemeMode.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .clickable { onThemeModeChange(mode) }
                .padding(vertical = MaterialTheme.dimens.margin.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlohomoraRadioButton(
                selected = themeMode == mode,
                onClick = { onThemeModeChange(mode) },
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            Text(
                text = when (mode) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DataSection(
    onClearTrustTokens: () -> Unit,
    onClearMutedEvents: () -> Unit,
    onResetPreferences: () -> Unit,
) {
    ClearAction(
        label = "Clear trusted devices",
        description = "Removes saved auth tokens. You will need to re-pair with each device.",
        confirmTitle = "Clear trusted devices?",
        confirmMessage = "All saved auth tokens will be removed. You will need to re-enter the OTP code when reconnecting to each device.",
        onConfirm = onClearTrustTokens,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

    ClearAction(
        label = "Clear muted events",
        description = "Unmutes all event names across all devices.",
        confirmTitle = "Clear muted events?",
        confirmMessage = "All muted event names will be restored. Previously hidden events will appear again.",
        onConfirm = onClearMutedEvents,
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

    ClearAction(
        label = "Reset all preferences",
        description = "Resets theme, mode, trusted devices, and muted events.",
        confirmTitle = "Reset all preferences?",
        confirmMessage = "Theme will revert to System default, all trusted devices and muted events will be cleared.",
        onConfirm = onResetPreferences,
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun ThemePicker(
    selectedId: String,
    isDark: Boolean,
    onSelect: (String) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        AlohomoraThemes.ids.forEach { id ->
            val theme =
                if (isDark) AlohomoraThemes.darkPreview(id) else AlohomoraThemes.lightPreview(id)
            val isSelected = id == selectedId
            ThemeCard(
                theme = theme,
                isSelected = isSelected,
                onClick = { onSelect(id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeCard(
    theme: AlohomoraColorTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val borderWidth =
        if (isSelected) MaterialTheme.dimens.stroke.medium else MaterialTheme.dimens.stroke.small

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .border(
                borderWidth,
                borderColor,
                MaterialTheme.shapes.small,
            )
            .clickable(onClick = onClick)
            .background(theme.materialColorScheme.surface)
            .padding(MaterialTheme.dimens.margin.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ColorDot(theme.accent)
            ColorDot(theme.success)
            ColorDot(theme.warning)
            ColorDot(theme.info)
            ColorDot(theme.fatal)
        }
        Text(
            text = theme.displayName,
            style = MaterialTheme.typography.labelSmall,
            color = theme.materialColorScheme.onSurface,
        )
    }
}

@Composable
private fun ColorDot(color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun ClearAction(
    label: String,
    description: String,
    confirmTitle: String,
    confirmMessage: String,
    onConfirm: () -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlohomoraAlertDialog(
            onDismissRequest = { showConfirm = false },
            title = confirmTitle,
            content = { Text(text = confirmMessage, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                AlohomoraTextButton(
                    text = "Clear",
                    onClick = {
                        onConfirm()
                        showConfirm = false
                    },
                    contentColor = MaterialTheme.colorScheme.error,
                )
            },
            dismissButton = {
                AlohomoraTextButton(text = "Cancel", onClick = { showConfirm = false })
            },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
        AlohomoraTextButton(
            text = "Clear",
            onClick = { showConfirm = true },
            contentColor = MaterialTheme.colorScheme.error,
        )
    }
}
