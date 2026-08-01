package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Asks for the code the device is displaying.
 *
 * Exists because the only OTP field used to live in the launcher dialog. Once the desktop began
 * retrying a dropped connection in place, a reconnect landed the device window in AwaitingAuth
 * with the sidebar reading "Waiting for OTP" and nowhere to type one — the session could never
 * be recovered without closing the window.
 *
 * Not dismissable by clicking away: the connection is stuck until this is answered, and an
 * accidental dismissal would leave the same dead end. Cancel disconnects explicitly.
 */
@Composable
fun OtpPromptDialog(
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var otp by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text("Authentication required", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    "Enter the 4-digit code shown on the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(MaterialTheme.dimens.margin.lg))
                AlohomoraTextField(
                    value = otp,
                    // Filtered rather than validated on submit: the code is always four digits,
                    // so anything else is a typo the user should not be able to make.
                    onValueChange = { input ->
                        if (input.length <= OTP_LENGTH && input.all(Char::isDigit)) otp = input
                    },
                    placeholder = "0000",
                    singleLine = true,
                    modifier = Modifier.width(160.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSubmit(otp); otp = "" },
                enabled = otp.length == OTP_LENGTH,
            ) { Text("Connect") }
        },
        dismissButton = {
            TextButton(onClick = { otp = ""; onCancel() }) { Text("Disconnect") }
        },
    )
}

private const val OTP_LENGTH = 4
