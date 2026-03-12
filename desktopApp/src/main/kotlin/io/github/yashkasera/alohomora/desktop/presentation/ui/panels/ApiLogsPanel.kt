package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.desktop.presentation.viewmodel.DevToolsViewModel
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLogsPanel(devToolsViewModel: DevToolsViewModel) {
    val logs by devToolsViewModel.apiLogs.collectAsState()
    var selectedLog by remember { mutableStateOf<TraceEntry?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(logs) { log ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLog = log }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "${log.method ?: "?"} ${log.path ?: log.url ?: ""}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = "status=${log.status ?: "-"}  duration=${log.duration ?: 0}ms",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    if (!log.message.isNullOrBlank()) {
                        Text(
                            text = log.message ?: "",
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                AlohomoraHorizontalDivider()
            }
        }

        if (selectedLog != null) {
//            ModalWideNavigationRail {
//                ApiLogDetailsContent(
//                    selectedLog!!
//                )
//            }
            ModalBottomSheet(
                onDismissRequest = {
                    selectedLog = null
                },
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight(),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "API Request Details",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            AlohomoraHorizontalDivider()

                            // Content
                            selectedLog?.let { log ->
                                /*ApiLogDetailsContent(
                                    call = log,
                                    modifier = Modifier.fillMaxSize(),
                                )*/
                            }
                        }
                    }
                },
            )
//            ModalDrawerSheet {
//                ApiLogDetailsContent(
//                    selectedLog!!
//                )
//            }
        }

    }
}
