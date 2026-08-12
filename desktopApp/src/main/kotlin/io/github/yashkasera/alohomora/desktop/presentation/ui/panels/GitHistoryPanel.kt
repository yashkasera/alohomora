package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.desktop.domain.model.GitHistoryCommit
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun GitHistoryPanel(devToolsViewModel: DevToolsViewModel) {
    val commits by devToolsViewModel.gitHistory.collectAsState()
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Git History",
                // Build metadata moved to its own Config section; this panel is commits only.
                subtitle = "Commits baked into the connected build",
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
                    GitHistoryRow(commit = commit)
                    AlohomoraHorizontalDivider()
                }
            }
        }
            ScrollToTopButton(lazyListState)
        }
    }
}

@Composable
private fun GitHistoryRow(commit: GitHistoryCommit) {
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
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = commit.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Text(
            text = commit.message,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = DateUtils.format(
                commit.timestamp,
                DateUtils.Format.READABLE_DATE_TIME,
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}
