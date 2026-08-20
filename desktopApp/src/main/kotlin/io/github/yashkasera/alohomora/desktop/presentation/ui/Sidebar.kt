package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.app.isMacOs
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection
import io.github.yashkasera.alohomora.desktop.domain.model.DeviceState
import io.github.yashkasera.alohomora.desktop.presentation.model.DeviceUi
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import io.github.yashkasera.alohomora.ui.icons.Android
import io.github.yashkasera.alohomora.ui.icons.Apple
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

private val SidebarTopInsetMac = 40.dp

@Composable
private fun CommandPaletteSearchPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Search,
            contentDescription = null,
            modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
        Text(
            text = "Search",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = if (isMacOs) "⌘K" else "Ctrl K",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ColumnScope.Sidebar(
    activeSection: DesktopSection,
    onDisconnect: () -> Unit,
    onSectionClick: (DesktopSection) -> Unit,
    connection: DevToolsConnection,
    devices: List<DeviceUi>,
    selectedDeviceId: String?,
    appName: String? = null,
    onOpenCommandPalette: () -> Unit = {},
    isModifierHeld: Boolean = false,
    visibleSections: List<DesktopSection> = emptyList(),
) {
    Icon(
        imageVector = Icons.AlohomoraFull,
        contentDescription = "Alohomora",
        modifier = Modifier
            .padding(top = if (isMacOs) SidebarTopInsetMac else MaterialTheme.dimens.margin.xxl)
            .padding(horizontal = MaterialTheme.dimens.margin.xxl)
            .width(148.dp),
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    CommandPaletteSearchPill(
        onClick = onOpenCommandPalette,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.margin.lg),
    )

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

    SidebarConnectionCard(
        connection = connection,
        devices = devices,
        selectedDeviceId = selectedDeviceId,
        appName = appName,
        onDisconnect = onDisconnect,
    )
    LazyColumn(
        modifier = Modifier
            .weight(1f),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        contentPadding = PaddingValues(MaterialTheme.dimens.margin.lg),
    ) {
        itemsIndexed(visibleSections, key = { _, section -> section.title }) { index, section ->
            NavigationDrawerItem(
                label = {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                },
                selected = activeSection == section,
                icon = {
                    Icon(
                        section.icon,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.icon.md),
                    )
                },
                badge = {
                    if (isModifierHeld && index < 9) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.shapes.extraSmall,
                                )
                                .padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                },
                onClick = { onSectionClick(section) },
            )
        }
    }
}

@Composable
private fun SidebarConnectionCard(
    connection: DevToolsConnection,
    devices: List<DeviceUi>,
    selectedDeviceId: String?,
    appName: String? = null,
    onDisconnect: () -> Unit,
) {
    val selectedOnlineDevice =
        devices.firstOrNull { it.id == selectedDeviceId && it.state == DeviceState.DEVICE }

    Column(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.dimens.margin.lg)
            .border(
                MaterialTheme.dimens.stroke.small,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.small,
            )
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.small)
            .fillMaxWidth()
            .padding(MaterialTheme.dimens.margin.md),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (selectedOnlineDevice == null) {
            Text(
                text = "No online devices found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotState = when (connection) {
                    DevToolsConnection.Disconnected -> ConnectionDotState.Disconnected
                    is DevToolsConnection.Connecting -> ConnectionDotState.Reconnecting
                    is DevToolsConnection.AwaitingAuth -> ConnectionDotState.Reconnecting
                    is DevToolsConnection.Connected -> ConnectionDotState.Connected
                    is DevToolsConnection.Reconnecting -> ConnectionDotState.Reconnecting
                    is DevToolsConnection.Failed -> ConnectionDotState.Disconnected
                }
                ConnectionStatusDot(state = dotState)
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                val connectionText = when (connection) {
                    DevToolsConnection.Disconnected -> "Disconnected"
                    is DevToolsConnection.Connecting -> "Connecting ${connection.host}:${connection.port}"
                    is DevToolsConnection.AwaitingAuth -> "Waiting for OTP"
                    is DevToolsConnection.Connected -> "Connected"
                    is DevToolsConnection.Reconnecting -> "Reconnecting (${connection.attempt})"

                    is DevToolsConnection.Failed -> "Failed: ${connection.reason}"
                }
                Text(
                    text = connectionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            if (!appName.isNullOrBlank()) {
                Text(
                    text = appName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 10.dp, vertical = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector =
                        if (selectedOnlineDevice.platform.isIos)
                            Icons.Apple
                        else Icons.Android,
                    contentDescription = if (selectedOnlineDevice.platform.isIos) "iOS" else "Android",
                    modifier = Modifier
                        .padding(MaterialTheme.dimens.margin.sm)
                        .size(MaterialTheme.dimens.icon.standard),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedOnlineDevice.model ?: selectedOnlineDevice.id,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = selectedOnlineDevice.id,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (connection is DevToolsConnection.Connected || connection is DevToolsConnection.Reconnecting) {
                    AlohomoraIconButton(onClick = onDisconnect) {
                        Icon(Icons.X, contentDescription = "Disconnect")
                    }
                }
            }
        }
    }
}
