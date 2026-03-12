package io.github.yashkasera.alohomora.presentation.ui.screens.trace.list

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.presentation.ui.components.EmptyState
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextFieldDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.ui.icons.ArrowLeft
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.trash
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasWhite
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TraceScreen(onTraceClick: (String) -> Unit) {
    val viewModel = koinViewModel<TraceViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AlohomoraTopBar(
                title = "Traffic Logs",
                navigationIcon = {
                    AlohomoraIconButton(onClick = {}) {
                        Icon(Icons.ArrowLeft, contentDescription = "back")
                    }
                },
                actions = {
                    AlohomoraIconButton(
                        onClick = {

                        },
                    ) {
                        Icon(
                            imageVector = Icons.trash,
                            contentDescription = "Clear All",
                        )
                    }
                },
            )
        },
        bottomBar = { TraceBottomBar() },
    ) { padding ->
        if (state.calls.isEmpty()) {
            EmptyState(
                icon = Icons.Server,
                title = "No Network Requests",
                subtitle = "Network requests will appear here as your app makes API calls",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(state.calls) { call ->
                    TraceItem(call = call, onClick = { onTraceClick(call.id) })
                    AlohomoraHorizontalDivider()
                }
                // Spacer to avoid bottom bar overlap if scaffold padding isn't enough (usually it is)
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun TraceHeader(requestCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Top Row: Back, Title, Delete
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlohomoraIconButton(onClick = { /* TODO: Back */ }) {
                Icon(Icons.ArrowLeft, contentDescription = "Back", tint = CanvasBlack)
            }

            Text(
                text = "Traffic Logs",
                style = MaterialTheme.typography.headlineMedium,
                fontStyle = FontStyle.Italic,
                color = CanvasBlack,
            )

            AlohomoraIconButton(onClick = { /* TODO: Clear */ }) {
                // Using Delete as DeleteSweep is extended
                Icon(Icons.trash, contentDescription = "Clear", tint = CanvasBlack)
            }
        }

        // Search & Filters
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            AlohomoraOutlinedTextField(
                value = "ewrweq",
                onValueChange = {},
                singleLine = true,
                shape = RectangleShape,
                colors = AlohomoraTextFieldDefaults.outlinedColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    cursorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                placeholder = {
                    Text("Search endpoints")
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // Filters & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                    FilterButton("Method", Icons.Default.KeyboardArrowDown)
//                    FilterButton("Status", Icons.Default.List) // List as FilterList proxy
                }

                Text(
                    text = "LIVE • $requestCount REQUESTS",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                    ),
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        AlohomoraHorizontalDivider()
    }
}

@Composable
fun FilterButton(label: String, icon: ImageVector) {
    Row(
        modifier = Modifier.border(1.dp, CanvasBlack.copy(alpha = 0.05f), CircleShape)
            .background(CanvasWhite, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = CanvasBlack,
        )
//        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = CanvasBlack)
    }
}

@Composable
fun TraceItem(call: TraceEntry, onClick: () -> Unit) {
    LaunchedEffect(Unit) {
        println(call)
    }
    val containerColor = if (call.isSuccessful.not()) MaterialTheme.colorScheme.errorContainer
    else MaterialTheme.colorScheme.background

    Column(
        modifier = Modifier.fillMaxWidth()
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MethodBadge(call.method.orEmpty())
                Text(
                    text = formatTime(call.time ?: 0),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "${call.duration}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )

                val statusColor = when {
                    call.isSuccessful -> MaterialTheme.colorScheme.onSurface // Design shows Black for 200 GET, Emerald for 201 etc. using Black for simplicity or custom logic
                    call.isViewed -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.error
                }
                // Override for 201 -> Emerald using theme color
                val finalStatusColor =
                    if (call.status == 201)
                        MaterialTheme.colorScheme.tertiary
                    else statusColor

                Text(
                    text = "${call.status}",
                    style = MaterialTheme.typography.labelSmall,
                    color = finalStatusColor,
                )
            }
        }

        Text(
            text = call.pathWithQuery,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 32.dp),
        )

        Text(
            text = "host: ${call.host}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MethodBadge(method: String) {
    // Design: POST/PUT/PATCH -> Black bg + White text. GET -> Border + Black text.
    val isWrite = method in listOf("POST", "PUT", "PATCH", "DELETE")

    val backgroundColor =
        if (isWrite) MaterialTheme.colorScheme.inverseSurface
        else Color.Transparent

    val contentColor = if (isWrite) MaterialTheme.colorScheme.inverseOnSurface
    else MaterialTheme.colorScheme.onSurface

    val borderModifier =
        if (!isWrite)
            Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface, RectangleShape)
        else Modifier

    Box(
        modifier = borderModifier.background(backgroundColor)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = method,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor,
        )
    }
}

@Composable
fun TraceBottomBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Pause Button
            AlohomoraFilledButton(
                modifier = Modifier.weight(1f),
                text = "Pause",
                onClick = {

                },
            )
            AlohomoraOutlinedButton(
                modifier = Modifier.weight(1f),
                text = "Export Har",
                onClick = {

                },
            )
            /*Box(
                modifier = Modifier.weight(1f)
                    .border(1.dp, CanvasBlack, RectangleShape)
                    .clickable { }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
//                    Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                    Text(
                        "PAUSE", style = TextStyle(
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                        )
                    )
                }
            }*/

            // Export HAR Button
            /*Box(modifier = Modifier.weight(2f).background(CanvasBlack).clickable { }
                .padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Download icon
//                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp), tint = CanvasWhite)
                    Text(
                        "EXPORT HAR", style = TextStyle(
                            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                        ), color = CanvasWhite
                    )
                }
            }*/
        }
    }
}

fun formatTime(timestamp: Long): String {
    try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        // HH:mm:ss.SS
        val h = local.hour.toString().padStart(2, '0')
        val m = local.minute.toString().padStart(2, '0')
        val s = local.second.toString().padStart(2, '0')
        val ms = (local.nanosecond / 1_000_000).toString().padStart(2, '0').take(2)
        return "$h:$m:$s.$ms"
    } catch (e: Exception) {
        return "00:00:00.00"
    }
}
