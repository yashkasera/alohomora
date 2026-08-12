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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.github.yashkasera.alohomora.desktop.app.ThemeMode
import io.github.yashkasera.alohomora.desktop.app.isShortcutModifier
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
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
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState(width = 600.dp, height = 720.dp)
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(MaterialTheme.dimens.margin.xl),
                ) {
                    SectionHeader("Appearance")

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                    ThemePicker(
                        selectedId = themeId,
                        isDark = isDark,
                        onSelect = onThemeIdChange,
                    )

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
                            RadioButton(
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

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    AlohomoraHorizontalDivider()

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    SectionHeader("Data")

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

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
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
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
            .clip(RoundedCornerShape(MaterialTheme.dimens.corner.medium))
            .border(
                borderWidth,
                borderColor,
                RoundedCornerShape(MaterialTheme.dimens.corner.medium),
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
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(text = confirmTitle, style = MaterialTheme.typography.titleMedium) },
            text = { Text(text = confirmMessage, style = MaterialTheme.typography.bodyMedium) },
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
