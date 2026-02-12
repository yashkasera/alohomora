package io.github.yashkasera.alohomora.presentation.ui.screens.commithistory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.data.model.Commit
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasDarkGray
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.ArrowLeft
import io.github.yashkasera.alohomora.presentation.ui.components.icons.gitGraph
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CommitHistoryScreen(
    onBackClick: () -> Unit,
) {
    val viewModel = koinViewModel<CommitHistoryViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            AlohomoraTopBar(
                title = "Commit History",
                subtitle = "SHOWING LAST ${state.commits.size} COMMITS",
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
            )
        },
        containerColor = CanvasWhite,
        contentColor = CanvasBlack,
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {

            // Commit List
            if (state.commits.isEmpty() && !state.isLoading) {
                EmptyState(
                    icon = Icons.gitGraph,
                    title = "No Commits Available",
                    subtitle = "Commit history will appear here",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.commits) { commit ->
                        CommitListItem(commit = commit)
                    }

                    /* // End of history indicator
                     item {
                         Box(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .padding(vertical = 32.dp),
                             contentAlignment = Alignment.Center,
                         ) {
                             Text(
                                 text = "END OF HISTORY",
                                 style = MaterialTheme.typography.labelSmall.copy(
                                     letterSpacing = 2.sp,
                                     fontSize = 10.sp,
                                 ),
                                 color = CanvasDarkGray.copy(alpha = 0.5f),
                             )
                         }
                     }*/
                }
            }
        }
    }
}

@Composable
private fun CommitListItem(
    commit: Commit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = CanvasLightGray, shape = RectangleShape)
            .background(CanvasWhite)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Commit SHA
                Text(
                    text = commit.sha.take(7),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = CanvasDarkGray.copy(alpha = 0.6f),
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Commit Message
                Text(
                    text = commit.message,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = CanvasBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Timestamp
                Text(
                    text = formatTimestamp(commit.timestamp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                    ),
                    color = CanvasDarkGray.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Author Section
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = commit.author.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    ),
                    color = CanvasBlack,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochSeconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val month = local.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val day = local.day
        val year = local.year
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        "$month $day, $year • $hour:$minute"
    } catch (e: Exception) {
        "Unknown date"
    }
}
