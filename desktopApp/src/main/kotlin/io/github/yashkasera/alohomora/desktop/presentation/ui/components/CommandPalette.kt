package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.app.displayModifier
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraSearchTextField
import io.github.yashkasera.alohomora.desktop.app.navigationShortcutDigit
import io.github.yashkasera.alohomora.desktop.presentation.ui.DesktopSection
import io.github.yashkasera.alohomora.ui.icons.Activity
import io.github.yashkasera.alohomora.ui.icons.Camera
import io.github.yashkasera.alohomora.ui.icons.CircleHelp
import io.github.yashkasera.alohomora.ui.icons.Eye
import io.github.yashkasera.alohomora.ui.icons.Globe
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.ui.theme.muted

enum class ActionCategory(val label: String) {
    NAVIGATION("Navigation"),
    GENERAL("General"),
    DEVICE("Device"),
    DATA("Data"),
}

data class CommandAction(
    val id: String,
    val label: String,
    val category: ActionCategory,
    val icon: ImageVector,
    val shortcutDisplay: String? = null,
    val enabled: Boolean = true,
    val action: () -> Unit,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CommandPalette(
    actions: List<CommandAction>,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val filtered = remember(query, actions) {
        if (query.isBlank()) {
            actions
        } else {
            actions.filter { it.label.contains(query, ignoreCase = true) }
        }
    }

    LaunchedEffect(filtered.size) {
        selectedIndex = selectedIndex.coerceIn(0, (filtered.size - 1).coerceAtLeast(0))
    }

    LaunchedEffect(selectedIndex) {
        if (filtered.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f))
            .clickable(indication = null, interactionSource = null, onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        Card(
            modifier = Modifier
                .padding(top = 80.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth(0.55f)
                .clickable(indication = null, interactionSource = null) {},
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            shape = MaterialTheme.shapes.medium,
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        ) {
            Column(
                modifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.DirectionDown -> {
                            if (filtered.isNotEmpty()) {
                                selectedIndex = (selectedIndex + 1).coerceAtMost(filtered.size - 1)
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filtered.isNotEmpty()) {
                                selectedIndex = (selectedIndex - 1).coerceAtLeast(0)
                            }
                            true
                        }
                        Key.Enter -> {
                            filtered.getOrNull(selectedIndex)?.let {
                                if (it.enabled) { onDismiss(); it.action() }
                            }
                            true
                        }
                        else -> false
                    }
                },
            ) {
                AlohomoraSearchTextField(
                    query = query,
                    onQueryChange = {
                        query = it
                        selectedIndex = 0
                    },
                    placeholder = "Search commands…",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MaterialTheme.dimens.margin.md)
                        .focusRequester(focusRequester),
                )

                AlohomoraHorizontalDivider()

                if (filtered.isEmpty()) {
                    Text(
                        "No matching commands",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.muted,
                        modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                            .padding(vertical = MaterialTheme.dimens.margin.xs),
                    ) {
                        var lastCategory: ActionCategory? = null
                        filtered.forEachIndexed { index, action ->
                            if (action.category != lastCategory) {
                                lastCategory = action.category
                                item(key = "header_${action.category.name}") {
                                    Text(
                                        text = action.category.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.muted,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(
                                            start = MaterialTheme.dimens.margin.lg,
                                            top = MaterialTheme.dimens.margin.md,
                                            bottom = MaterialTheme.dimens.margin.xs,
                                        ),
                                    )
                                }
                            }
                            item(key = action.id) {
                                CommandActionRow(
                                    action = action,
                                    isSelected = index == selectedIndex,
                                    onClick = {
                                        if (action.enabled) {
                                            onDismiss()
                                            action.action()
                                        }
                                    },
                                )
                            }
                        }
                    }

                    AlohomoraHorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = MaterialTheme.dimens.margin.lg,
                                vertical = MaterialTheme.dimens.margin.sm,
                            ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
                    ) {
                        FooterHint("↑↓", "Navigate")
                        FooterHint("↵", "Select")
                        FooterHint("Esc", "Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterHint(key: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ShortcutChip(key)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.muted,
        )
    }
}

@Composable
private fun CommandActionRow(
    action: CommandAction,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest
                else MaterialTheme.colorScheme.surface,
            )
            .clickable(enabled = action.enabled, onClick = onClick)
            .padding(
                horizontal = MaterialTheme.dimens.margin.lg,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimens.icon.md),
            tint = if (action.enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.muted,
        )
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (action.enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.muted,
            modifier = Modifier.weight(1f),
        )
        if (action.shortcutDisplay != null) {
            ShortcutChip(action.shortcutDisplay)
        }
    }
}

@Composable
fun ShortcutChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                MaterialTheme.shapes.extraSmall,
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

fun buildCommandActions(
    visibleSections: List<DesktopSection>,
    activeSection: DesktopSection,
    onSectionChange: (DesktopSection) -> Unit,
    isConnected: Boolean,
    packageName: String?,
    selectedDeviceId: String?,
    isAndroid: Boolean,
    onToggleTheme: () -> Unit,
    onShowHelp: () -> Unit,
    onClearTraffic: () -> Unit,
    onClearTraces: () -> Unit,
    onClearEvents: () -> Unit,
    onForceStop: () -> Unit,
    onLaunchApp: () -> Unit,
    onClearAppData: () -> Unit,
    onTakeScreenshot: () -> Unit,
    onRebootDevice: () -> Unit,
    onToggleWifi: () -> Unit,
    onToggleMobileData: () -> Unit,
    onClearLogcat: () -> Unit,
): List<CommandAction> {
    val mod = displayModifier()
    val actions = mutableListOf<CommandAction>()

    visibleSections.forEachIndexed { index, section ->
        actions += CommandAction(
            id = "nav_${section.name}",
            label = section.title,
            category = ActionCategory.NAVIGATION,
            icon = section.icon,
            shortcutDisplay = navigationShortcutDigit(index)?.let { "$mod+$it" },
            enabled = true,
            action = { onSectionChange(section) },
        )
    }

    actions += CommandAction(
        id = "general_theme",
        label = "Toggle Theme",
        category = ActionCategory.GENERAL,
        icon = Icons.Eye,
        shortcutDisplay = "$mod+T",
        action = onToggleTheme,
    )
    actions += CommandAction(
        id = "general_help",
        label = "Keyboard Shortcuts",
        category = ActionCategory.GENERAL,
        icon = Icons.CircleHelp,
        shortcutDisplay = "$mod+/",
        action = onShowHelp,
    )

    if (isAndroid) {
        val hasPackage = !packageName.isNullOrBlank()
        val deviceReady = isConnected && !selectedDeviceId.isNullOrBlank()

        actions += CommandAction(
            id = "device_screenshot",
            label = "Take Screenshot",
            category = ActionCategory.DEVICE,
            icon = Icons.Camera,
            shortcutDisplay = "$mod+Shift+S",
            enabled = deviceReady,
            action = onTakeScreenshot,
        )
        actions += CommandAction(
            id = "device_force_stop",
            label = "Force Stop App",
            category = ActionCategory.DEVICE,
            icon = Icons.X,
            enabled = deviceReady && hasPackage,
            action = onForceStop,
        )
        actions += CommandAction(
            id = "device_launch",
            label = "Launch App",
            category = ActionCategory.DEVICE,
            icon = Icons.Play,
            enabled = deviceReady && hasPackage,
            action = onLaunchApp,
        )
        actions += CommandAction(
            id = "device_clear_data",
            label = "Clear App Data",
            category = ActionCategory.DEVICE,
            icon = Icons.Trash,
            enabled = deviceReady && hasPackage,
            action = onClearAppData,
        )
        actions += CommandAction(
            id = "device_reboot",
            label = "Reboot Device",
            category = ActionCategory.DEVICE,
            icon = Icons.RefreshCw,
            enabled = deviceReady,
            action = onRebootDevice,
        )
        actions += CommandAction(
            id = "device_wifi",
            label = "Toggle Wi-Fi",
            category = ActionCategory.DEVICE,
            icon = Icons.Globe,
            enabled = deviceReady,
            action = onToggleWifi,
        )
        actions += CommandAction(
            id = "device_data",
            label = "Toggle Mobile Data",
            category = ActionCategory.DEVICE,
            icon = Icons.Activity,
            enabled = deviceReady,
            action = onToggleMobileData,
        )
        actions += CommandAction(
            id = "device_clear_logcat",
            label = "Clear Logcat",
            category = ActionCategory.DEVICE,
            icon = Icons.Trash,
            enabled = deviceReady,
            action = onClearLogcat,
        )
    }

    actions += CommandAction(
        id = "data_clear_traffic",
        label = "Clear Traffic",
        category = ActionCategory.DATA,
        icon = Icons.Trash,
        enabled = isConnected,
        action = onClearTraffic,
    )
    actions += CommandAction(
        id = "data_clear_traces",
        label = "Clear Traces",
        category = ActionCategory.DATA,
        icon = Icons.Trash,
        enabled = isConnected,
        action = onClearTraces,
    )
    actions += CommandAction(
        id = "data_clear_events",
        label = "Clear Events",
        category = ActionCategory.DATA,
        icon = Icons.Trash,
        enabled = isConnected,
        action = onClearEvents,
    )

    return actions
}
