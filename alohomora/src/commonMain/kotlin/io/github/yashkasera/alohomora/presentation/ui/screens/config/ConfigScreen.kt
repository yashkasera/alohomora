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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.data.model.BuildMetadata
import io.github.yashkasera.alohomora.data.model.toBuildMetadata
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
        content()
    }
}

@Composable
private fun BuildInfoGrid(buildConfig: BuildMetadata?) {
    AlohomoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.Config.BUILD_INFO),
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
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
}

@Composable
private fun EnvironmentDetails(environment: String) {
    val iconTint = MaterialTheme.colorScheme.primary

    AlohomoraCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AlohomoraTestTags.Config.ENVIRONMENT),
        shape = MaterialTheme.shapes.extraLarge,
        colors = AlohomoraCardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }
            Column {
                Text(
                    text = environment,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "Current environment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
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
        Text(
            text = value ?: "--not-set--",
            style = MaterialTheme.typography.bodyMedium,
            color = if (value != null)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview
@Composable
private fun InfoItemPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                InfoItem(label = "Branch", value = "feat/cache-editing")
                InfoItem(label = "Commit Hash", value = null)
            }
        }
    }
}

@Preview
@Composable
private fun ConfigSectionPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            ConfigSection(title = "BUILD INFORMATION") {
                BuildInfoGrid(
                    buildConfig = BuildMetadata(
                        branch = "main",
                        commitSha = "a1b2c3d4e5f6",
                        isDirty = false,
                        buildTimestampUtc = 1724198400000L,
                        variantName = "debug",
                        versionName = "1.2.0",
                        versionCode = 12,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ConfigScreenContentPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                ConfigSection(title = "BUILD INFORMATION") {
                    BuildInfoGrid(
                        buildConfig = BuildMetadata(
                            branch = "feat/cache-editing",
                            commitSha = "4303677abc123",
                            isDirty = true,
                            buildTimestampUtc = 1724198400000L,
                            variantName = "debug",
                            versionName = "2.0.0",
                            versionCode = 20,
                        ),
                    )
                }

                AlohomoraHorizontalDivider(
                    modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.md),
                )

                ConfigSection(title = "ENVIRONMENT") {
                    EnvironmentDetails(environment = "productionDebug")
                }

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.huge))
            }
        }
    }
}

@Preview
@Composable
private fun BuildInfoNullValuesPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.lg)) {
                ConfigSection(title = "BUILD INFORMATION") {
                    BuildInfoGrid(buildConfig = null)
                }
            }
        }
    }
}
