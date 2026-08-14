package io.github.yashkasera.alohomora.desktop.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
internal fun NoDevicePanel(
    onRefresh: () -> Unit,
) {
    EmptyState(
        icon = Icons.HardDrive,
        title = "No device connected",
        subtitle = "Connect an Android device over USB or adb tcpip, or an iPhone over USB, then refresh.",
        action = {
            AlohomoraOutlinedButton(text = "Refresh devices", onClick = onRefresh)
        },
    )
}

@Composable
internal fun DeviceErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(MaterialTheme.dimens.margin.md)
            .widthIn(max = 720.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        tonalElevation = MaterialTheme.dimens.margin.xs,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f, fill = false),
            )
            AlohomoraTextButton(
                text = "Dismiss",
                onClick = onDismiss,
                uppercase = false,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
internal fun ReconnectingBanner(
    attempt: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(MaterialTheme.dimens.margin.md)
            .widthIn(max = 480.dp),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        tonalElevation = MaterialTheme.dimens.margin.xs,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.dimens.margin.md,
                vertical = MaterialTheme.dimens.margin.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            ConnectionStatusDot(state = ConnectionDotState.Reconnecting)
            Text(
                text = "Reconnecting (attempt $attempt)...",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
