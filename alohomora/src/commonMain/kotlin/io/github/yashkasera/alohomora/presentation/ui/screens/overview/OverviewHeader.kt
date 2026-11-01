package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.data.model.AlohomoraConfig
import io.github.yashkasera.alohomora.data.model.GitHistoryCommit
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import kotlin.time.Clock

/**
 * Scrolls away with the grid rather than pinning as a top bar — the hero status card carries all
 * live state, so a pinned header is dead weight once the user scrolls.
 *
 * [AlohomoraImpl.config] stays null-safe: the Gradle plugin is not applied to `:alohomora` itself,
 * so config is null in the library's own device tests.
 *
 * [onClose] is null wherever the host already has a way out (Android's system back), and the close
 * button is rendered only when it is not. It lives here, on the root screen, because that is the one
 * place every path through the console returns to.
 */
@Composable
internal fun OverviewHeader(
    modifier: Modifier = Modifier,
    config: AlohomoraConfig? = null,
    onClose: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = MaterialTheme.dimens.margin.xl),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AlohomoraFull,
                modifier = Modifier.width(180.dp),
                contentDescription = null,
            )
            if (onClose != null) {
                Spacer(Modifier.weight(1f))
                AlohomoraIconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag(AlohomoraTestTags.Overview.CLOSE),
                ) {
                    Icon(imageVector = Icons.X, contentDescription = "Close Alohomora")
                }
            }
        }
        config?.let { config ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            ) {
                config.appName?.let { appName ->
                    Text(
                        modifier = Modifier.weight(1f),
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AlohomoraChip(
                    label = "v${config.versionName}",
                    uppercase = false,
                )
            }
        }
    }
}

@Preview
@Composable
private fun OverviewHeaderPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xl)) {
                // Config is process-global, so the preview shows whatever the host provides —
                // typically the config-null variant (wordmark only) in the library itself.
                OverviewHeader(
                    config = object : AlohomoraConfig {
                        override val packageName: String
                            get() = ""
                        override val versionName: String
                            get() = "1.0.0"
                        override val versionCode: Int
                            get() = 1000
                        override val variantName: String
                            get() = ""
                        override val flavorName: String?
                            get() = ""
                        override val buildType: String?
                            get() = ""
                        override val branch: String
                            get() = ""
                        override val commitSha: String
                            get() = ""
                        override val isDirty: Boolean
                            get() = false
                        override val buildTimestampUtc: Long
                            get() = Clock.System.now().epochSeconds
                        override val slackWebhookUrl: String?
                            get() = ""
                        override val commits: List<GitHistoryCommit>
                            get() = emptyList()
                    }
                )
            }
        }
    }
}
