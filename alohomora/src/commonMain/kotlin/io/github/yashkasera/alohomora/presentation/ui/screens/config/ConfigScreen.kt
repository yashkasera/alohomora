package io.github.yashkasera.alohomora.presentation.ui.screens.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.data.model.BuildMetadata
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import kotlin.time.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun ConfigScreen(
    onBackClick: () -> Unit = {},
    onSaveConfig: (String) -> Unit = {},
) {
    var beUrl by remember { mutableStateOf("https://api.production.example.com") }
    var isEditing by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            ConfigurationTopBar(
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            ConfigurationHeader()

            Spacer(modifier = Modifier.height(32.dp))

            // Backend URL Configuration (Editable)
            ConfigSection(title = "BACKEND CONFIGURATION") {
                BackendUrlConfig(
                    url = beUrl,
                    isEditing = isEditing,
                    onUrlChange = { beUrl = it },
                    onEditClick = { isEditing = true },
                    onSaveClick = {
                        isEditing = false
                        onSaveConfig(beUrl)
                    },
                    onCancelClick = { isEditing = false },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            AlohomoraHorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            // Build Information (Read-only)
            ConfigSection(title = "BUILD INFORMATION") {
                BuildInfoGrid(buildConfig = Alohomora.buildInfo)
            }

            Spacer(modifier = Modifier.height(32.dp))
            AlohomoraHorizontalDivider()
            Spacer(modifier = Modifier.height(32.dp))

            // Environment Details (Read-only)
            ConfigSection(title = "ENVIRONMENT") {
                EnvironmentDetails("debug")
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

// ============================================================================
// Top Bar
// ============================================================================

@Composable
private fun ConfigurationTopBar(
    onBackClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AlohomoraIconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp),
        ) {
            Text("←", style = MaterialTheme.typography.headlineMedium)
        }

        Text(
            text = "Configuration",
            style = MaterialTheme.typography.titleLarge,
        )

        // Empty space for symmetry
        Spacer(modifier = Modifier.width(48.dp))
    }
}

// ============================================================================
// Header Section
// ============================================================================

@Composable
private fun ConfigurationHeader() {
    Column {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "DEVELOPER CONFIGURATION",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ============================================================================
// Config Section Container
// ============================================================================

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

// ============================================================================
// Backend URL Configuration
// ============================================================================

@Composable
private fun BackendUrlConfig(
    url: String,
    isEditing: Boolean,
    onUrlChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Base URL",
                style = MaterialTheme.typography.titleMedium,
            )

            if (!isEditing) {
                AlohomoraTextButton(
                    text = "Edit",
                    onClick = onEditClick,
                    size = io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize.SMALL,
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isEditing) {
            AlohomoraOutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium,
                placeholder = {
                    Text(
                        text = "Enter backend URL...",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                singleLine = true,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                AlohomoraTextButton(
                    text = "Cancel",
                    onClick = onCancelClick,
                    size = io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize.SMALL,
                )

                AlohomoraFilledButton(
                    text = "Save",
                    onClick = onSaveClick,
                    size = io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize.SMALL,
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
            ) {
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

// ============================================================================
// Build Information Grid
// ============================================================================

@Composable
private fun BuildInfoGrid(buildConfig: BuildMetadata?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InfoItem(
                label = "Branch",
                value = buildConfig?.branch,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            InfoItem(
                label = "Build Variant",
                value = buildConfig?.buildVariant,
                modifier = Modifier.weight(1f),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            InfoItem(
                label = "Version Name",
                value = buildConfig?.versionName,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            InfoItem(
                label = "Version Code",
                value = buildConfig?.versionCode?.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        InfoItem(
            label = "Build Time",
            value =
                buildConfig?.buildTimestampUtc?.let(Instant::fromEpochSeconds)
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())?.format(
                        LocalDateTime.Format {
                            monthNumber(); char('/');
                            day(); char('/');
                            year(); char(' ');
                            hour(); char(':');
                            minute(); char(':');
                            second();
                        },
                    ),
            modifier = Modifier.fillMaxWidth(),
        )

        InfoItem(
            label = "Commit Hash",
            value = buildConfig?.commitSha,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ============================================================================
// Environment Details
// ============================================================================

@Composable
private fun EnvironmentDetails(environment: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.primary),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = environment.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Current environment configuration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

// ============================================================================
// Info Item Component
// ============================================================================

@Composable
private fun InfoItem(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = value ?: "--not-set--",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
