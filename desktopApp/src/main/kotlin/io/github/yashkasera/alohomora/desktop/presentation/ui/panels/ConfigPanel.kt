package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel

@Composable
fun ConfigPanel(devToolsViewModel: DevToolsViewModel) {
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (buildInfo == null) {
            Text(
                text = "No build config available. Connect a device to load build metadata.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return
        }

        val info = buildInfo ?: return
        InfoRow(label = "Project", value = info.projectName)
        InfoRow(label = "Version", value = "${info.versionName} (${info.versionCode})")
        InfoRow(label = "Variant", value = info.variantName)
        InfoRow(label = "Environment", value = buildEnvironment(info))
        InfoRow(label = "Branch", value = info.branch)
        InfoRow(label = "Commit", value = info.commitSha)
        InfoRow(label = "Dirty", value = if (info.isDirty) "Yes" else "No")
        InfoRow(
            label = "Build Time",
            value = DateUtils.format(
                info.buildTimestampUtc,
                DateUtils.Format.READABLE_DATE_TIME,
                DateUtils.TimeUnit.SECONDS,
            ),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value ?: "-",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun buildEnvironment(info: BuildInfo): String {
    val parts = mutableListOf<String>()
    if (!info.flavorName.isNullOrBlank()) {
        parts.add(info.flavorName)
    }
    if (info.variantName.isNotBlank()) {
        parts.add(info.variantName)
    }
    if (!info.buildType.isNullOrBlank()) {
        parts.add(info.buildType)
    }
    return if (parts.isEmpty()) "-" else parts.joinToString(" • ")
}
