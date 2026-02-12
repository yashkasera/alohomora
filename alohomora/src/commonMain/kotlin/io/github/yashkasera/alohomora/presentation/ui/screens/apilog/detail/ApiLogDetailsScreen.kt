package io.github.yashkasera.alohomora.presentation.ui.screens.apilog.detail

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.data.entity.ApiRequest
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopBar
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.ArrowLeft
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Search
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Server
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Share
import io.github.yashkasera.alohomora.presentation.ui.components.icons.clock
import io.github.yashkasera.alohomora.presentation.ui.components.icons.refreshCw
import kotlinx.coroutines.launch
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ApiLogDetailsScreen(callId: String, onBackClick: () -> Unit = {}) {
    val viewModel = koinViewModel<ApiLogDetailsViewModel> { parametersOf(callId) }
    val state by viewModel.state.collectAsState()
    val call = state.call

    if (call == null) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Log not found.",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        return
    }

    val tabs = listOf("OVERVIEW", "REQUEST", "RESPONSE")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        topBar = {
            AlohomoraTopBar(
                title = "API Request",
                subtitle = null,
                navigationIcon = {
                    AlohomoraIconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.ArrowLeft,
                            contentDescription = "back",
                        )
                    }
                },
                actions = {
                    AlohomoraIconButton(
                        onClick = {
                            // TODO: Implement share
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Share,
                            contentDescription = "Share",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Tab Row
            AlohomoraPrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                tabs.forEachIndexed { index, title ->
                    AlohomoraTab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = title,
                    )
                }
            }

//            HorizontalDivider()

            // HorizontalPager for tab content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(top = 24.dp),
                ) {
                    when (page) {
                        0 -> OverviewTab(call = call)
                        1 -> RequestTab(call = call)
                        2 -> ResponseTab(call = call)
                    }
                }
            }

            // Replay button (sticky bottom)
            ReplayButton(
                onClick = { /* TODO: Implement replay */ },
            )
        }
    }
}

// ============================================================================
// Overview Tab
// ============================================================================

@Composable
private fun OverviewTab(call: ApiRequest) {
    Column {
        // Method badge and endpoint
        MethodAndEndpointSection(
            method = call.method.orEmpty(),
            url = call.pathWithQuery.orEmpty(),
            requestId = "${call.id}",
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Hero stats (STATUS, LATENCY, FORMAT)
        HeroStatsSection(
            statusCode = call.status ?: 0,
            latencyMs = call.duration ?: 0,
            format = "JSON", // TODO: Detect from content-type
        )

        AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

        // Info rows
        InfoRowsSection(call = call)

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ============================================================================
// Request Tab
// ============================================================================

@Composable
private fun RequestTab(call: ApiRequest) {
    Column {
        // Method badge and endpoint
        MethodAndEndpointSection(
            method = call.method.orEmpty(),
            url = call.url.orEmpty(),
            requestId = "#${call.id}",
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Request headers section
        call.requestHeaders?.let {
            SectionHeader(title = "REQUEST HEADERS")
            Spacer(modifier = Modifier.height(16.dp))
            CodeViewer(json = it.toString())

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Request body section
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "REQUEST BODY")
            Spacer(modifier = Modifier.width(8.dp))
            FormatBadge(format = "RAW")
        }

        Spacer(modifier = Modifier.height(16.dp))
        CodeViewer(json = call.request ?: "{}")

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            AlohomoraOutlinedButton(
                text = "Copy JSON",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(16.dp))
            AlohomoraOutlinedButton(
                text = "Share",
                onClick = { /* TODO */ },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ============================================================================
// Response Tab
// ============================================================================

@Composable
private fun ResponseTab(call: ApiRequest) {
    var prettifyJson by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    Column {
        // Prettify JSON toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "PRETTIFY JSON",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(8.dp))
            PrettifyToggle(
                checked = prettifyJson,
                onCheckedChange = { prettifyJson = it },
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            currentMatch = 1,
            totalMatches = 3,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Response metadata (status, format, size)
        ResponseMetadata(
            statusCode = call.status ?: 0,
            format = "JSON",
            sizeKb = calculateJsonSize(call.response ?: "{}"),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // JSON viewer with syntax highlighting
        JsonViewer(
            json = call.response ?: "{}",
            prettify = prettifyJson,
            searchQuery = searchQuery,
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

// ============================================================================
// Shared Components
// ============================================================================

@Composable
private fun MethodAndEndpointSection(
    method: String,
    url: String,
    requestId: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MethodBadgeDetails(method = method)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Text(
            text = "ID: #$requestId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun MethodBadgeDetails(method: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color = MaterialTheme.colorScheme.primary)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = method.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun HeroStatsSection(
    statusCode: Int,
    latencyMs: Long,
    format: String,
) {
    Column {
        // Status section (large)
        Text(
            text = "STATUS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = statusCode.toString(),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = getStatusText(statusCode),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Latency and Format row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "LATENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = latencyMs.toString(),
                        style = MaterialTheme.typography.displaySmall,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ms",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }

            Column {
                Text(
                    text = "FORMAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = format,
                    style = MaterialTheme.typography.displaySmall,
                )
            }
        }
    }
}

@Composable
private fun InfoRowsSection(call: ApiRequest) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        InfoRow(
            icon = Icons.clock,
            label = "TIMESTAMP",
            value = formatTimestamp(call.time ?: 0),
        )

        InfoRow(
            icon = Icons.Server,
            label = "HOST",
            value = call.host,
        )

        InfoRow(
            icon = Icons.refreshCw,
            label = "CLIENT",
            value = "android-v33 (1.0.4)", // TODO: Get from user agent
        )
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value ?: "--not-set--",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun FormatBadge(format: String) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.primary)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = format,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun CodeViewer(json: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
    ) {
        Text(
            text = json,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ReplayButton(onClick: () -> Unit) {
    AlohomoraFilledButton(
        text = "Replay Request",
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .height(56.dp),
        shape = RectangleShape,
        content = {
            // TODO: Add replay icon
            Text(text = "↻", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "REPLAY REQUEST",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    )
}

// ============================================================================
// Response Tab Specific Components
// ============================================================================

@Composable
private fun PrettifyToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .height(24.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .clickable { onCheckedChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surface),
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentMatch: Int,
    totalMatches: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Search,
            contentDescription = "back",
        )

        // Search input (simplified - in production use TextField)
        Text(
            text = query.ifEmpty { "Search..." },
            style = MaterialTheme.typography.bodyMedium,
            color = if (query.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f),
        )

        // Match counter and navigation
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "$currentMatch of $totalMatches",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AlohomoraIconButton(
                onClick = { /* Navigate to previous match */ },
                modifier = Modifier.size(24.dp),
            ) {
                Text("↑", style = MaterialTheme.typography.labelMedium)
            }

            AlohomoraIconButton(
                onClick = { /* Navigate to next match */ },
                modifier = Modifier.size(24.dp),
            ) {
                Text("↓", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ResponseMetadata(
    statusCode: Int,
    format: String,
    sizeKb: Double,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status badge
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "$statusCode ${getStatusText(statusCode)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            // Format badge
            Text(
                text = format,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Size
        Text(
            text = "${formatSize(sizeKb)} KB",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun JsonViewer(
    json: String,
    prettify: Boolean,
    searchQuery: String,
) {
    val displayJson = if (prettify) prettifyJson(json) else json

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        JsonText(
            json = displayJson,
            searchQuery = searchQuery,
        )
    }
}

@Composable
private fun JsonText(
    json: String,
    searchQuery: String,
) {
    if (searchQuery.isEmpty()) {
        Text(
            text = json,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    } else {
        // Split text and highlight matches
        val parts = json.split(searchQuery, ignoreCase = true)
        val matches = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
            .findAll(json)
            .map { it.value }
            .toList()

        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                parts.forEachIndexed { index, part ->
                    Text(
                        text = part,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (index < matches.size) {
                        Text(
                            text = matches[index],
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

private fun getStatusText(statusCode: Int): String {
    return when (statusCode) {
        200 -> "OK"
        201 -> "Created"
        204 -> "No Content"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        else -> ""
    }
}

private fun formatTimestamp(timestamp: Long): String {
    return try {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${dateTime.year}-${dateTime.monthNumber.toString().padStart(2, '0')}-${
            dateTime.dayOfMonth.toString().padStart(2, '0')
        } ${dateTime.hour.toString().padStart(2, '0')}:${
            dateTime.minute.toString().padStart(2, '0')
        }:${dateTime.second.toString().padStart(2, '0')}.${
            dateTime.nanosecond.toString().take(3)
        }"
    } catch (e: Exception) {
        "Invalid timestamp"
    }
}

private fun extractHost(url: String): String {
    return try {
        val hostPattern = Regex("(?:https?://)?([^/]+)")
        hostPattern.find(url)?.groupValues?.get(1) ?: url
    } catch (e: Exception) {
        url
    }
}

private fun calculateJsonSize(json: String): Double {
    return json.encodeToByteArray().size.toDouble() / 1024.0
}

private fun formatSize(sizeKb: Double): String {
    val rounded = (sizeKb * 10).toInt() / 10.0
    return "$rounded"
}

private fun prettifyJson(json: String): String {
    return try {
        // Simple JSON prettifier - indents nested structures
        var result = ""
        var indent = 0
        var inString = false

        json.forEach { char ->
            when {
                char == '"' && result.lastOrNull() != '\\' -> {
                    inString = !inString
                    result += char
                }

                !inString && char == '{' || char == '[' -> {
                    result += char
                    result += "\n"
                    indent++
                    result += "  ".repeat(indent)
                }

                !inString && char == '}' || char == ']' -> {
                    result += "\n"
                    indent--
                    result += "  ".repeat(indent)
                    result += char
                }

                !inString && char == ',' -> {
                    result += char
                    result += "\n"
                    result += "  ".repeat(indent)
                }

                !inString && char == ':' -> {
                    result += char
                    result += " "
                }

                char == ' ' && !inString -> {
                    // Skip spaces outside strings
                }

                else -> result += char
            }
        }
        result
    } catch (e: Exception) {
        json
    }
}
