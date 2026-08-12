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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.yashkasera.alohomora.common.DateUtils
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.data.model.BuildMetadata
import io.github.yashkasera.alohomora.data.model.toBuildMetadata
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens
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
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AlohomoraTopBar(
                title = "Config",
                navigationIcon = {
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(imageVector = Icons.ArrowLeft, contentDescription = "back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MaterialTheme.dimens.margin.xl),
        ) {
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxxl))

            // Build Information (Read-only)
            ConfigSection(title = "BUILD INFORMATION") {
                BuildInfoGrid(buildConfig = Alohomora.config?.toBuildMetadata())
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxxl))
            AlohomoraHorizontalDivider()
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxxl))

            // Environment Details (Read-only)
            Alohomora.config?.let {
                ConfigSection(title = "ENVIRONMENT") {
                    EnvironmentDetails(
                        environment = buildString {
                            if (it.flavorName.isNullOrBlank().not()) {
                                append("${it.flavorName?.lowercase()}")
                                append(
                                    it.variantName.lowercase().replaceFirstChar { it.uppercase() },
                                )
                            } else {
                                append(it.variantName)
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.huge))
        }
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
            // tertiary, matching PORT/DEVICE/OTP on Overview and DATABASE/TABLES in the Vault.
            // This screen was the only one rendering section labels in muted grey.
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
        content()
    }
}

// ============================================================================
// Build Information Grid
// ============================================================================

@Composable
private fun BuildInfoGrid(buildConfig: BuildMetadata?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
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
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
            InfoItem(
                label = "Build Variant",
                value = buildConfig?.variantName,
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
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
            InfoItem(
                label = "Version Code",
                value = buildConfig?.versionCode?.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        InfoItem(
            label = "Build Time",
            // DateUtils, not a hand-rolled kotlinx.datetime call. This screen used
            // fromEpochSeconds on a value that is milliseconds, rendering the year as +58553 —
            // and being the one timestamp not going through DateUtils is why nothing caught it.
            value = buildConfig?.buildTimestampUtc
                ?.let { DateUtils.format(it, DateUtils.Format.ISO_DATE_TIME_SECONDS) },
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
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(MaterialTheme.dimens.margin.lg),
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
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
            Text(
                text = environment,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

        Text(
            text = "Current environment configuration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
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
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = MaterialTheme.dimens.margin.md, vertical = MaterialTheme.dimens.margin.sm),
        ) {
            Text(
                text = value ?: "--not-set--",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
