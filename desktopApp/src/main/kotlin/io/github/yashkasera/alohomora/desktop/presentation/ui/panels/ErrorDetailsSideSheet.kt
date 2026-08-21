package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Error
import io.github.yashkasera.alohomora.common.exceptionTypeName
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.KeyValueRow
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.LocalCopyFeedback
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SectionLabel
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SlackShareDialog
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Slack
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

private const val ERROR_SHEET_WIDTH_FRACTION = 0.45f

@Composable
fun ErrorDetailsSideSheet(
    error: Error?,
    devToolsViewModel: DevToolsViewModel,
    onDismiss: () -> Unit,
) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val copyFeedback = LocalCopyFeedback.current
    val slackShareError by devToolsViewModel.slackShareError.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val isSlackConfigured = buildInfo?.slackWebhookUrl.isNullOrBlank().not()

    var showSlackShareDialog by remember(error?.id) { mutableStateOf(false) }

    val shareText = error?.let {
        buildString {
            appendLine("Error: ${it.exceptionTypeName()}")
            it.reason?.takeIf(String::isNotBlank)?.let { r -> appendLine("Reason: $r") }
            appendLine("Time: ${DateUtils.format(it.time, DateUtils.Format.ISO_DATE_TIME_SECONDS)}")
            it.place?.takeIf(String::isNotBlank)?.let { p -> appendLine("Place: $p") }
            it.stackTrace?.takeIf(String::isNotBlank)?.let { st ->
                appendLine()
                appendLine("Stack trace:")
                appendLine(st)
            }
        }
    }.orEmpty()

    AlohomoraSideSheet(
        visible = error != null,
        onDismiss = onDismiss,
        widthFraction = ERROR_SHEET_WIDTH_FRACTION,
        header = {
            error?.let { selected ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = MaterialTheme.dimens.margin.xl,
                            vertical = MaterialTheme.dimens.margin.md,
                        ),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selected.exceptionTypeName(),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            AlohomoraChip(
                                label = "FATAL",
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Text(
                            text = DateUtils.format(selected.time, DateUtils.Format.ISO_DATE_TIME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    AlohomoraIconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(shareText))
                            copyFeedback("Copied to clipboard")
                        },
                    ) {
                        Icon(imageVector = Icons.Copy, contentDescription = "Copy error")
                    }
                    AlohomoraIconButton(
                        onClick = { showSlackShareDialog = true },
                    ) {
                        Icon(imageVector = Icons.Slack, contentDescription = "Share to Slack")
                    }
                    AlohomoraIconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.X, contentDescription = "Close")
                    }
                }
            }
        },
    ) {
        error?.let { selected ->
            ErrorDetailsContent(error = selected)
        }
    }

    error?.let { selected ->
        if (showSlackShareDialog) {
            SlackShareDialog(
                isConfigured = isSlackConfigured,
                currentWebhookUrl = buildInfo?.slackWebhookUrl,
                shareError = slackShareError,
                onDismiss = {
                    showSlackShareDialog = false
                    devToolsViewModel.clearSlackShareError()
                },
                onShareText = { email ->
                    devToolsViewModel.shareErrorToSlack(selected, email) {
                        showSlackShareDialog = false
                    }
                },
                shareTextLabel = "Share Error to Slack",
                onClearError = devToolsViewModel::clearSlackShareError,
            )
        }
    }
}

@Composable
private fun ColumnScope.ErrorDetailsContent(error: Error) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(MaterialTheme.dimens.margin.xl),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        KeyValueRow(label = "Type", value = error.exceptionTypeName())
        KeyValueRow(
            label = "Time",
            value = DateUtils.format(error.time, DateUtils.Format.ISO_DATE_TIME),
        )

        error.reason?.takeIf(String::isNotBlank)?.let { reason ->
            KeyValueRow(label = "Reason", value = reason)
        }

        error.place?.takeIf(String::isNotBlank)?.let { place ->
            KeyValueRow(label = "Place", value = place)
        }

        val trace = error.stackTrace?.takeIf(String::isNotBlank)
        if (trace != null) {
            SectionLabel("Stack Trace")
            AlohomoraCodeBlock(
                content = trace,
            )
        } else {
            SectionLabel("Stack Trace")
            Text(
                text = "No stack trace was recorded for this error.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
