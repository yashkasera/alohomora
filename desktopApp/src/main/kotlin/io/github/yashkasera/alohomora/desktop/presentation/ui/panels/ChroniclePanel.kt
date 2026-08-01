package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.desktop.domain.model.BuildInfo
import io.github.yashkasera.alohomora.desktop.domain.model.ChronicleCommit
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.Icons

@Composable
fun ChroniclePanel(devToolsViewModel: DevToolsViewModel) {
    val commits by devToolsViewModel.chronicle.collectAsState()
    val buildInfo by devToolsViewModel.buildInfo.collectAsState()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Chronicle",
                subtitle = "Build info generated using the Alohomora Plugin",
                showDivider = lazyListState.canScrollBackward,
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Box(modifier = Modifier.padding(it).fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            stickyHeader {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(MaterialTheme.dimens.margin.md),
                ) {
                    Text(
                        text = "Build Info",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            item {
                OutlinedCard(
                    modifier = Modifier.padding(MaterialTheme.dimens.margin.xxl),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.md, vertical = MaterialTheme.dimens.margin.sm),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
                    ) {
                        if (buildInfo == null) {
                            Text(
                                text = "No build config available. Connect a device to load build metadata.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            return@Column
                        }

                        val info = buildInfo ?: return@Column
                        InfoRow(label = "Project", value = info.projectName)
                        InfoRow(
                            label = "Version",
                            value = "${info.versionName} (${info.versionCode})",
                        )
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
                            ),
                        )
                    }
                }
            }
            stickyHeader {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(MaterialTheme.dimens.margin.md),
                ) {
                    Text(
                        text = "Commit History",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Showing last ${commits.size} commits",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }

            if (commits.isEmpty()) {
                item {
                    EmptyState(
                        // No .padding(it): the enclosing Box already applied the Scaffold
                        // insets, so passing them again double-padded this.
                        icon = Icons.GitGraph,
                        title = "No Git Commits",
                        subtitle = "No commits available. Connect a device to load commit history.",
                    )
                }
            } else {
                items(commits) { commit ->
                    ChronicleRow(commit = commit)
                    AlohomoraHorizontalDivider()
                }
            }
        }
            ScrollToTopButton(lazyListState)
        }
    }
}

@Composable
private fun ChronicleRow(commit: ChronicleCommit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.dimens.margin.lg, vertical = MaterialTheme.dimens.margin.md),
        verticalArrangement = Arrangement.spacedBy(6.dp), // 6.dp intentional: tight commit row gap
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = commit.sha.take(7),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = commit.author,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = commit.message,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = DateUtils.format(
                commit.timestamp,
                DateUtils.Format.READABLE_DATE_TIME,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
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
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
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
