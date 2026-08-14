package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import io.github.yashkasera.alohomora.desktop.app.MacTitleBarHeight
import io.github.yashkasera.alohomora.desktop.app.applyMacTitleBar
import io.github.yashkasera.alohomora.desktop.app.isMacOs
import io.github.yashkasera.alohomora.desktop.mcp.PendingConfirmation
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun McpConfirmationDialog(
    pending: PendingConfirmation,
    isDarkState: MutableState<Boolean>,
    themeId: String,
) {
    DialogWindow(
        title = "Alohomora",
        onCloseRequest = { pending.resolve(false) },
        state = rememberDialogState(width = 440.dp, height = 220.dp),
        resizable = false,
    ) {
        LaunchedEffect(Unit) {
            window.isAlwaysOnTop = true
            window.toFront()
            window.requestFocus()
        }
        AppTheme(isDarkState = isDarkState, themeId = themeId) {
            applyMacTitleBar(window)
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = if (isMacOs) MacTitleBarHeight else 0.dp)
                        .padding(MaterialTheme.dimens.margin.xxl),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                ) {
                    Text(pending.title, style = MaterialTheme.typography.titleMedium)
                    Text(pending.message, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(
                            MaterialTheme.dimens.margin.sm,
                            Alignment.End,
                        ),
                    ) {
                        AlohomoraOutlinedButton(
                            text = "Deny",
                            size = AlohomoraButtonSize.SMALL,
                            onClick = { pending.resolve(false) },
                        )
                        AlohomoraFilledButton(
                            text = "Allow",
                            onClick = { pending.resolve(true) },
                        )
                    }
                }
            }
        }
    }
}
