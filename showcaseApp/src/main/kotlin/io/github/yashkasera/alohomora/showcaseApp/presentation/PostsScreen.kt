package io.github.yashkasera.alohomora.showcaseApp.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Icons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AndroidSampleApp(
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        PostsScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostsScreen(
    viewModel: PostsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alohomora Sample") },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    throw IllegalStateException()
                },
            ) {
                Icon(
                    imageVector = Icons.AlertTriangle,
                    contentDescription = null,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                PreferencesSection(
                    username = state.preferences.username,
                    autoRefresh = state.preferences.autoRefresh,
                    lastRefreshMillis = state.preferences.lastRefreshEpochMillis,
                    onUsernameChange = viewModel::updateUsername,
                    onAutoRefreshToggle = viewModel::updateAutoRefresh,
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Posts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Button(onClick = viewModel::refreshPosts) {
                        Text(if (state.isLoading) "Refreshing..." else "Refresh")
                    }
                }
            }

            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(state.posts) { post ->
                PostCard(
                    title = post.title,
                    body = post.body,
                    onClick = { viewModel.onPostClicked(post.id) },
                )
            }
        }
    }
}

@Composable
private fun PreferencesSection(
    username: String,
    autoRefresh: Boolean,
    lastRefreshMillis: Long,
    onUsernameChange: (String) -> Unit,
    onAutoRefreshToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = username,
                onValueChange = onUsernameChange,
                label = { Text("Username") },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Auto refresh")
                Switch(checked = autoRefresh, onCheckedChange = onAutoRefreshToggle)
            }

            Text(
                text = "Last refresh: ${formatTime(lastRefreshMillis)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PostCard(
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "Never"
    val formatter = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
