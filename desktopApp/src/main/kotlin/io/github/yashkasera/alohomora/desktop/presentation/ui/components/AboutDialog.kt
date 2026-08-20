package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import io.github.yashkasera.alohomora.desktop.app.DesktopBuildConfig
import io.github.yashkasera.alohomora.desktop.app.MacTitleBarHeight
import io.github.yashkasera.alohomora.desktop.app.applyMacTitleBar
import io.github.yashkasera.alohomora.desktop.app.isMacOs
import io.github.yashkasera.alohomora.desktop.app.isShortcutModifier
import io.github.yashkasera.alohomora.desktop.domain.service.UpdateInfo
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.icons.Alohomora
import io.github.yashkasera.alohomora.ui.icons.Globe
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import java.awt.Desktop
import java.net.URI

private const val REPO_URL = "https://github.com/yashkasera/alohomora"

@Composable
fun AboutDialog(
    isDark: Boolean,
    themeId: String = "default",
    updateInfo: UpdateInfo?,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState(width = 420.dp, height = 380.dp)
    DialogWindow(
        title = "About Alohomora",
        state = state,
        onCloseRequest = onDismiss,
        resizable = false,
    ) {
        AppTheme(initialIsDark = isDark, themeId = themeId) {
            applyMacTitleBar(window)
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) { focusRequester.requestFocus() }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                        when (event.key) {
                            Key.Escape -> {
                                onDismiss(); true
                            }
                            Key.W if event.isShortcutModifier() -> {
                                onDismiss(); true
                            }
                            else -> false
                        }
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        // Clear the macOS traffic lights that overlay the transparent title bar.
                        .padding(top = if (isMacOs) MacTitleBarHeight else 0.dp)
                        .padding(MaterialTheme.dimens.margin.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Alohomora,
                        contentDescription = "Alohomora",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    Text(
                        text = "Alohomora",
                        style = MaterialTheme.typography.headlineSmall,
                    )

                    Text(
                        text = "v${DesktopBuildConfig.version}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    if (updateInfo != null && updateInfo.isUpdateAvailable) {
                        AlohomoraTextButton(
                            text = "v${updateInfo.latestVersion} available",
                            onClick = {
                                try {
                                    Desktop.getDesktop().browse(URI(updateInfo.htmlUrl))
                                } catch (_: Exception) {
                                }
                            },
                            uppercase = false,
                            contentColor = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    AlohomoraHorizontalDivider()

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    Text(
                        text = "Developer observability toolkit for Android and iOS apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    InfoRow(label = "Author", value = "Yash Kasera")
                    InfoRow(label = "License", value = "Apache 2.0")

                    Spacer(modifier = Modifier.weight(1f))

                    AlohomoraTextButton(
                        text = "GitHub",
                        onClick = {
                            try {
                                Desktop.getDesktop().browse(URI(REPO_URL))
                            } catch (_: Exception) {
                            }
                        },
                        uppercase = false,
                        leadingIcon = {
                            Icon(
                                Icons.Globe,
                                contentDescription = null,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MaterialTheme.dimens.margin.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
