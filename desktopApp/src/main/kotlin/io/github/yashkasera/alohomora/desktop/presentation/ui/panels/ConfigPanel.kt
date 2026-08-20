package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.TopBarLayout
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Settings
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Which build the connected app actually is.
 *
 * A section of its own because it previously had none: the same values were rendered inside the Git
 * History panel and inside the Dashboard, and the Dashboard is gated on `DEVICE_METRICS` — so for an
 * iOS device the build metadata was reachable only by opening a panel named after something else.
 */
@Composable
fun ConfigPanel(devToolsViewModel: DevToolsViewModel) {
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Config",
                layout = TopBarLayout.START_ALIGNED,
                subtitle = "Build metadata from the connected app",
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            val info = buildInfo
            if (info == null) {
                EmptyState(
                    icon = Icons.Settings,
                    title = "No build metadata",
                    subtitle = "Apply the Alohomora Gradle plugin on Android, or add the " +
                        "build-info script phase on iOS, then rebuild the app.",
                    setup = "plugins {\n    id(\"io.github.yashkasera.alohomora\")\n}",
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(MaterialTheme.dimens.margin.xxl),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl),
                ) {
                    ConfigCard(title = "Application") {
                        ConfigRow("Project", info.appName)
                        ConfigRow("Package", info.packageName)
                        ConfigRow("Version", "${info.versionName} (${info.versionCode})")
                        ConfigRow("Variant", info.variantName)
                        ConfigRow("Environment", buildEnvironment(info))
                    }

                    ConfigCard(title = "Source") {
                        ConfigRow("Branch", info.branch)
                        ConfigRow("Commit", info.commitSha)
                        WorkingTreeRow(isDirty = info.isDirty)
                        ConfigRow(
                            "Built",
                            DateUtils.format(
                                info.buildTimestampUtc,
                                DateUtils.Format.READABLE_DATE_TIME,
                            ),
                        )
                    }

                    ConfigCard(title = "Integrations") {
                        // Presence only. The webhook is a live secret, and rendering it here would
                        // put it on screen during any screen-share or recorded demo.
                        ConfigRow(
                            "Slack",
                            if (info.slackWebhookUrl.isNullOrBlank()) {
                                "Not configured"
                            } else {
                                "Webhook configured"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfigCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
        )
        AlohomoraOutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.md,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ConfigRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "-",
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Dirty is the one field worth a colour: it means the build does not correspond to any commit, so a
 * bug reproduced against it cannot be pinned to [BuildInfo.commitSha].
 */
@Composable
private fun WorkingTreeRow(isDirty: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Working tree",
            style = MaterialTheme.typography.labelMedium,
        )
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
        if (isDirty) {
            AlohomoraChip(
                label = "Dirty",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            Text(text = "Clean", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

internal fun buildEnvironment(info: BuildInfo): String {
    val parts = mutableListOf<String>()
    if (!info.flavorName.isNullOrBlank()) parts.add(info.flavorName)
    if (info.variantName.isNotBlank()) parts.add(info.variantName)
    if (!info.buildType.isNullOrBlank()) parts.add(info.buildType)
    return if (parts.isEmpty()) "-" else parts.joinToString(" • ")
}
