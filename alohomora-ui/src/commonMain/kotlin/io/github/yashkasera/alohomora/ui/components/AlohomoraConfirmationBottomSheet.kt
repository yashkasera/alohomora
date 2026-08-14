package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags

/**
 * Configuration for the confirmation bottom sheet.
 *
 * @property title The title text to display
 * @property message The message/description text
 * @property confirmButtonText Text for the confirm button
 * @property dismissButtonText Text for the dismiss button
 * @property icon Optional icon to display above the title
 * @property confirmButtonColor Color for the confirm button (default is error color for destructive actions)
 * @property isDestructive Whether this is a destructive action (affects button colors)
 */
data class ConfirmationConfig(
    val title: String,
    val message: String,
    val confirmButtonText: String = "Confirm",
    val dismissButtonText: String = "Cancel",
    val icon: ImageVector? = null,
    val confirmButtonColor: Color? = null,
    val isDestructive: Boolean = false,
)

/**
 * A generic confirmation bottom sheet that can be used across the app for
 * destructive actions, confirmations, or any action requiring user verification.
 *
 * @param config Configuration for the confirmation dialog
 * @param onConfirm Callback when user confirms the action
 * @param onDismiss Callback when user dismisses the sheet (or taps outside)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmationBottomSheet(
    config: ConfirmationConfig,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val confirmColor = config.confirmButtonColor
        ?: if (config.isDestructive) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    AlohomoraBottomSheetModal(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Optional icon
            config.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 16.dp),
                    tint = if (config.isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            }

            // Title
            Text(
                text = config.title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Message
            Text(
                text = config.message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Dismiss/Cancel button
                AlohomoraOutlinedButton(
                    modifier = Modifier.weight(1f)
                        .testTag(AlohomoraTestTags.Chrome.CONFIRM_DISMISS),
                    text = config.dismissButtonText,
                    onClick = onDismiss,
                )

                // Confirm button. Tagged here rather than by the caller: every confirmation in the
                // console goes through this sheet, so one tag covers all of them, and the button
                // labels ("Clear All", "Delete") are not stable enough to address by text.
                AlohomoraFilledButton(
                    modifier = Modifier.weight(1f).testTag(AlohomoraTestTags.Chrome.CONFIRM_ACCEPT),
                    text = config.confirmButtonText,
                    onClick = {
                        onConfirm()
                        onDismiss()
                    },
                    containerColor = confirmColor,
                    contentColor = if (config.isDestructive) {
                        MaterialTheme.colorScheme.onError
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                )
            }
        }
    }
}

/**
 * Convenience overload for simple confirmations without custom configuration.
 *
 * @param title The title text to display
 * @param message The message/description text
 * @param onConfirm Callback when user confirms the action
 * @param onDismiss Callback when user dismisses the sheet
 * @param confirmButtonText Text for the confirm button
 * @param dismissButtonText Text for the dismiss button
 * @param isDestructive Whether this is a destructive action
 */
@Composable
fun ConfirmationBottomSheet(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "Confirm",
    dismissButtonText: String = "Cancel",
    isDestructive: Boolean = false,
) {
    ConfirmationBottomSheet(
        config = ConfirmationConfig(
            title = title,
            message = message,
            confirmButtonText = confirmButtonText,
            dismissButtonText = dismissButtonText,
            isDestructive = isDestructive,
        ),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}
