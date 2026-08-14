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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.data.model.BuildMetadata
import io.github.yashkasera.alohomora.data.model.toBuildMetadata
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
internal fun ConfigScreen(
    onBackClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Config",
                navigationIcon = {
                    AlohomoraIconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.BACK),
                    ) {
                        Icon(Icons.ArrowLeft, contentDescription = "Back")
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
                .testTag(AlohomoraTestTags.Config.ROOT),
        ) {
            ConfigSection(title = "BUILD INFORMATION") {
                BuildInfoGrid(buildConfig = Alohomora.config?.toBuildMetadata())
            }

            AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.md))

            Alohomora.config?.let {
                ConfigSection(title = "ENVIRONMENT") {
                    EnvironmentDetails(
                        environment = buildString {
                            if (it.flavorName.isNullOrBlank().not()) {
                                append("${it.flavorName?.lowercase()}")
                                append(
                                    it.variantName.lowercase()
                                        .replaceFirstChar { variant -> variant.uppercase() },
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

@Composable
private fun ConfigSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(MaterialTheme.dimens.margin.md)
            .fillMaxWidth(),
    ) {
        Text(
            text = title,
            // tertiary, matching PORT/DEVICE/OTP on Overview and DATABASE/TABLES in the Vault.
            // This screen was the only one rendering section labels in muted grey.
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
        content()
    }
}

@Composable
private fun BuildInfoGrid(buildConfig: BuildMetadata?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.Config.BUILD_INFO),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
    ) {
        InfoItem(
            label = "Branch",
            value = buildConfig?.branch,
        )

        InfoItem(
            label = "Build Variant",
            value = buildConfig?.variantName,
        )

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

@Composable
private fun EnvironmentDetails(environment: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.Config.ENVIRONMENT)
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

@Composable
private fun InfoItem(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.testTag(AlohomoraTestTags.Config.info(label))) {
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
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(
                    horizontal = MaterialTheme.dimens.margin.md,
                    vertical = MaterialTheme.dimens.margin.sm,
                ),
        ) {
            Text(
                text = value ?: "--not-set--",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
