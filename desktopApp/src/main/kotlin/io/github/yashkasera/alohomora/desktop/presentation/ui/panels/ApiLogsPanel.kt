package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider

@Composable
fun ApiLogsPanel(devToolsViewModel: DevToolsViewModel) {
    val logs by devToolsViewModel.apiLogs.collectAsState()
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(logs) { log ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(
                    text = "${log.method ?: "?"} ${log.path ?: log.url ?: ""}",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "status=${log.status ?: "-"}  duration=${log.duration ?: 0}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                if (!log.message.isNullOrBlank()) {
                    Text(
                        text = log.message ?: "",
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            AlohomoraHorizontalDivider()
        }
    }
}
