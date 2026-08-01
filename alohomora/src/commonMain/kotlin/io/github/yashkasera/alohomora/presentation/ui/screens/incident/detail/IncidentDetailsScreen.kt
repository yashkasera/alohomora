package io.github.yashkasera.alohomora.presentation.ui.screens.incident.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.Copy
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.ui.icons.Share
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun IncidentDetailsScreen(
    incidentId: Long,
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<IncidentDetailsViewModel> { parametersOf(incidentId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "BACK",
                subtitle = "REPORT #${incidentId}",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) { padding ->
        if (state.isLoading || state.incident == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
            }
        } else {
            val incident = state.incident ?: return@Scaffold

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Header Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = MaterialTheme.dimens.margin.xl, vertical = MaterialTheme.dimens.margin.xxl),
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(MaterialTheme.dimens.corner.small))
                            .padding(horizontal = MaterialTheme.dimens.margin.md, vertical = 6.dp),
                    ) {
                        Text(
                            text = "FATAL EXCEPTION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            ),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                    Text(
                        text = incident.reason?.substringAfterLast(".")?.substringBefore(":")
                            ?: "Unknown Exception",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontStyle = FontStyle.Italic,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

                    Text(
                        text = incident.reason ?: "No additional information available",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontStyle = FontStyle.Italic,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetadataItem(
                                label = "DEVICE",
                                value = "Unknown",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
                            MetadataItem(
                                label = "ANDROID VERSION",
                                value = "API 34\n(UpsideDownCake)",
                                modifier = Modifier.weight(1f),
                            )
                        }

                        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            MetadataItem(
                                label = "APP VERSION",
                                value = "Unknown",
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
                            MetadataItem(
                                label = "TIMESTAMP",
                                value = DateUtils.format(incident.time, DateUtils.Format.MONTH_DAY_TIME),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                // Stack Trace Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = MaterialTheme.dimens.margin.xl),
                ) {
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "FULL STACKTRACE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${incident.stackTrace?.lines()?.size ?: 0} lines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(MaterialTheme.dimens.stroke.small, MaterialTheme.colorScheme.outline)
                            .padding(MaterialTheme.dimens.margin.lg),
                    ) {
                        Text(
                            text = incident.stackTrace ?: "No stack trace available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        AlohomoraOutlinedButton(
                            text = "Copy Trace",
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(incident.stackTrace ?: incident.reason ?: ""),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            borderColor = MaterialTheme.colorScheme.onSurface,
                        ) {
                            Icon(
                                Icons.Copy,
                                contentDescription = null,
                                modifier = Modifier.padding(end = MaterialTheme.dimens.margin.sm),
                            )
                            Text(
                                "COPY TRACE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))

                        AlohomoraFilledButton(
                            text = "Copy Report",
                            onClick = {
                                clipboard.setText(
                                    AnnotatedString(
                                        buildString {
                                            appendLine(incident.reason ?: "Unknown exception")
                                            appendLine(incident.place ?: "")
                                            appendLine()
                                            append(incident.stackTrace ?: "")
                                        },
                                    ),
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RectangleShape,
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ) {
                            Icon(
                                Icons.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = MaterialTheme.dimens.margin.sm),
                            )
                            Text(
                                "SHARE REPORT",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
private fun MetadataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontStyle = FontStyle.Italic,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
