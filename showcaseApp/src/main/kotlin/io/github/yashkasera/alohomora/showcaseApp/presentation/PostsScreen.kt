package io.github.yashkasera.alohomora.showcaseApp.presentation

import android.content.Intent
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.showcaseApp.ShowcaseTestTags
import io.github.yashkasera.alohomora.showcaseApp.WebViewActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AndroidSampleApp() {
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // The caught path, and the one instrumentation tests drive. `recordError` is also
                // the API most consumers actually reach for — the crash handler below only exists
                // for the failures nobody caught.
                FloatingActionButton(
                    onClick = {
                        try {
                            error("Handled failure to demo Alohomora.recordError")
                        } catch (e: IllegalStateException) {
                            Alohomora.recordError(e, place = "PostsScreen")
                        }
                    },
                    modifier = Modifier.testTag(ShowcaseTestTags.RECORD_ERROR),
                ) {
                    // A label rather than an icon from :alohomora-ui. This app compiles against
                    // alohomora-noop in release, and the no-op module deliberately does not depend
                    // on the design system, so an Alohomora ImageVector here breaks the release
                    // variant.
                    Text("Record")
                }

                // Genuinely uncaught: this reaches the installed crash handler and kills the
                // process. Deliberately never tapped by an instrumentation test — doing so takes
                // the whole test class down with it.
                FloatingActionButton(
                    onClick = {
                        throw IllegalStateException("Intentional crash to demo Alohomora error capture")
                    },
                    modifier = Modifier.testTag(ShowcaseTestTags.CRASH),
                ) {
                    Text("Crash")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .testTag(ShowcaseTestTags.POSTS_LIST),
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
                WebViewSection()
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
                    Button(
                        onClick = viewModel::refreshPosts,
                        modifier = Modifier.testTag(ShowcaseTestTags.REFRESH),
                    ) {
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
                        modifier = Modifier.testTag(ShowcaseTestTags.ERROR_MESSAGE),
                    )
                }
            }

            items(state.posts) { post ->
                PostCard(
                    title = post.title,
                    body = post.body,
                    onClick = { viewModel.onPostClicked(post.id) },
                    modifier = Modifier.testTag(ShowcaseTestTags.post(post.id)),
                )
            }
        }
    }
}

@Composable
private fun WebViewSection() {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "WebView (VPN Throttle Demo)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Opens a WebView that bypasses OkHttp/Ktor interceptors. " +
                    "Enable device-wide VPN throttle from the desktop to see it throttled.",
                style = MaterialTheme.typography.bodySmall,
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(context, WebViewActivity::class.java)
                            .putExtra(WebViewActivity.EXTRA_URL, "https://www.wikipedia.org"),
                    )
                },
                modifier = Modifier.fillMaxWidth().testTag(ShowcaseTestTags.OPEN_WEBVIEW),
            ) {
                Text("Open Wikipedia")
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
                modifier = Modifier.fillMaxWidth().testTag(ShowcaseTestTags.USERNAME),
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
                Switch(
                    checked = autoRefresh,
                    onCheckedChange = onAutoRefreshToggle,
                    modifier = Modifier.testTag(ShowcaseTestTags.AUTO_REFRESH),
                )
            }

            Text(
                text = "Last refresh: ${formatTime(lastRefreshMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag(ShowcaseTestTags.LAST_REFRESH),
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
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
