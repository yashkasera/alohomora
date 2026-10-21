package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.yashkasera.alohomora.ui.components.AlohomoraAlertDialog
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton

/**
 * Confirms sharing a config artifact with the team before the branch + review is created.
 *
 * Share was a one-click action that silently created a branch and a pull/merge request — which reads
 * as irreversible and scary to a non-engineer. The confirmation turns it into a deliberate choice and
 * says, in plain language, that nothing changes for the team until a reviewer approves.
 */
@Composable
fun ConfigShareConfirmDialog(
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlohomoraAlertDialog(
        onDismissRequest = onDismiss,
        title = "Share with your team?",
        content = {
            Text(
                text = "Share \"$itemName\" with your team? This opens a review your teammates can " +
                    "approve — nothing changes for them until they do.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            AlohomoraTextButton(text = "Share", onClick = onConfirm)
        },
        dismissButton = {
            AlohomoraTextButton(text = "Cancel", onClick = onDismiss)
        },
    )
}
