package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import androidx.compose.runtime.Composable
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton

/**
 * Confirms deleting captured data.
 *
 * Confirmed rather than immediate because the delete reaches the device: an accidental click
 * throws away a capture that may not be reproducible, and there is no undo. Mirrors the
 * ConfirmationBottomSheet the mobile console already uses for the same action.
 */
@Composable
fun ClearCapturedDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlohomoraAlertDialog(
        onDismissRequest = onDismiss,
        title = title,
        content = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            AlohomoraTextButton(
                text = "Clear",
                onClick = onConfirm,
                contentColor = MaterialTheme.colorScheme.error,
            )
        },
        dismissButton = {
            AlohomoraTextButton(text = "Cancel", onClick = onDismiss)
        },
    )
}
