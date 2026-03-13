package io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.clock
import io.github.yashkasera.alohomora.ui.icons.refreshCw
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import kotlin.math.pow
import kotlin.time.Instant
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TraceDetailsContent(
    trace: TraceEntry,
    modifier: Modifier = Modifier,
) {
    val tabs = listOf("OVERVIEW", "REQUEST", "RESPONSE")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Hoist scroll states to persist across tab switches
    val overviewScrollState = rememberScrollState()
    val requestScrollState = rememberScrollState()
    val responseScrollState = rememberScrollState()

    // Use derivedStateOf to batch tab selection updates and reduce recompositions
    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        // Tab Row
        AlohomoraPrimaryTabRow(
            selectedTabIndex = currentPage,
            modifier = Modifier.fillMaxWidth(),
        ) {
            tabs.forEachIndexed { index, title ->
                AlohomoraTab(
                    selected = currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = title,
                )
            }
        }

        // HorizontalPager for tab content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
        ) { page ->
            val scrollState = when (page) {
                0 -> overviewScrollState
                1 -> requestScrollState
                2 -> responseScrollState
                else -> rememberScrollState()
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp),
            ) {
                when (page) {
                    0 -> OverviewTab(trace = trace)
                    1 -> RequestTab(trace = trace)
                    2 -> ResponseTab(trace = trace)
                }
            }
        }

    }
}

// ============================================================================
// Overview Tab
// ============================================================================

@Composable
private fun OverviewTab(trace: TraceEntry) {
    // Method badge and endpoint
    MethodAndEndpointSection(
        method = trace.method.orEmpty(),
        url = trace.pathWithQuery,
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Calculate sizes from content
    val requestSize = trace.requestSize ?: 0
    val responseSize = trace.responseSize ?: 0L

    // Detect format from Content-Type header
    val responseFormat = detectFormatFromContentType(trace.responseHeaders)

    // Hero stats (STATUS, LATENCY, SIZE, FORMAT)
    HeroStatsSection(
        statusCode = trace.status ?: 0,
        latencyMs = trace.duration ?: 0,
        responseSize = responseSize,
        requestSize = requestSize,
        format = responseFormat,
    )

    AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = 32.dp))

    // Info rows with all TraceEntry fields
    InfoRowsSection(trace = trace)

    Spacer(modifier = Modifier.height(48.dp))
}

// ============================================================================
// Request Tab
// ============================================================================

@Composable
private fun RequestTab(trace: TraceEntry) {
    Column {
        // Method badge and endpoint
        MethodAndEndpointSection(
            method = trace.method.orEmpty(),
            url = trace.url.orEmpty(),
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Request headers section with formatted display
        trace.requestHeaders?.let { headers ->
            val formattedHeaders = formatHeaders(headers)
            val headerCount = headers.values.sumOf { it.size }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(title = "REQUEST HEADERS")
                Spacer(modifier = Modifier.width(8.dp))
                CountBadge(count = headerCount)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display formatted headers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
            ) {
                formattedHeaders.forEach { headerLine ->
                    Text(
                        text = headerLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Request body section
        val requestFormat = detectFormatFromContentType(trace.requestHeaders)
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "REQUEST BODY")
            Spacer(modifier = Modifier.width(8.dp))
            FormatBadge(format = requestFormat.uppercase())
        }

        Spacer(modifier = Modifier.height(16.dp))
        CodeViewer(json = trace.request ?: "{}")

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
private fun ResponseTab(trace: TraceEntry) {
    var prettifyJson by remember { mutableStateOf(true) }

    // Calculate size and detect format
    val responseSize = trace.responseSize ?: 0L
    val responseFormat = detectFormatFromContentType(trace.responseHeaders)

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

        // Response metadata (status, format, size)
        ResponseMetadata(
            statusCode = trace.status ?: 0,
            format = responseFormat,
            sizeBytes = responseSize,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Response Headers section
        trace.responseHeaders?.let { headers ->
            val formattedHeaders = formatHeaders(headers)
            val headerCount = headers.values.sumOf { it.size }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(title = "RESPONSE HEADERS")
                Spacer(modifier = Modifier.width(8.dp))
                CountBadge(count = headerCount)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display formatted headers
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp),
            ) {
                formattedHeaders.forEach { headerLine ->
                    Text(
                        text = headerLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // JSON viewer with syntax highlighting
        JsonViewer(
            json = trace.response ?: "{}",
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
    responseSize: Long,
    requestSize: Long,
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

        // Latency, Size, and Format row
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

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatBytes(responseSize),
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "(${formatBytes(requestSize)} up)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
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
private fun InfoRowsSection(trace: TraceEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        InfoRow(
            icon = Icons.clock,
            label = "TIMESTAMP",
            value = formatTimestamp(trace.time ?: 0),
        )

        InfoRow(
            icon = Icons.Server,
            label = "HOST",
            value = trace.host,
        )

        InfoRow(
            icon = Icons.refreshCw,
            label = "SCHEME",
            value = trace.scheme?.uppercase() ?: "HTTPS",
        )

        InfoRow(
            icon = Icons.refreshCw,
            label = "CLIENT",
            value = "android-v33 (1.0.4)", // TODO: Get from user agent
        )

        // Additional TraceEntry fields
        trace.query?.let { query ->
            if (query.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Search,
                    label = "QUERY PARAMS",
                    value = query,
                )
            }
        }
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
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
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
            modifier = Modifier.fillMaxWidth(),
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
            Text(text = "↻", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "REPLAY REQUEST",
                style = MaterialTheme.typography.labelLarge,
            )
        },
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
private fun ResponseMetadata(
    statusCode: Int,
    format: String,
    sizeBytes: Long,
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

            Text(
                text = format,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            text = formatBytes(sizeBytes),
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
    val displayJson = remember(json, prettify) {
        if (prettify) prettifyJson(json) else json
    }

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
        val highlightedParts = remember(json, searchQuery) {
            val parts = json.split(searchQuery, ignoreCase = true)
            val matches = Regex(Regex.escape(searchQuery), RegexOption.IGNORE_CASE)
                .findAll(json)
                .map { it.value }
                .toList()
            parts to matches
        }

        val (parts, matches) = highlightedParts

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
        "${dateTime.year}-${(dateTime.month.ordinal + 1).toString().padStart(2, '0')}-${dateTime.dayOfMonth.toString().padStart(2, '0')} ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}:${dateTime.second.toString().padStart(2, '0')}.${dateTime.nanosecond.toString().take(3)}"
    } catch (e: Exception) {
        "Invalid timestamp"
    }
}

/**
 * Formats bytes to human-readable format (B, KB, MB, GB)
 */
private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "0 B"
    if (bytes == 0L) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val unitIndex = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
        .coerceAtMost(units.size - 1)

    val value = bytes / 1024.0.pow(unitIndex)

    return when {
        unitIndex == 0 -> "$bytes ${units[unitIndex]}"
        value >= 100 -> "${value.toInt()} ${units[unitIndex]}"
        value >= 10 -> "${kotlin.math.round(value * 10) / 10} ${units[unitIndex]}"
        else -> "${kotlin.math.round(value * 100) / 100} ${units[unitIndex]}"
    }
}

/**
 * Formats headers map into a list of formatted key-value strings
 */
private fun formatHeaders(headers: Map<String, List<String>>?): List<String> {
    if (headers == null) return emptyList()

    return headers.flatMap { (key, values) ->
        values.map { value ->
            "$key: $value"
        }
    }
}

/**
 * Detects content format from Content-Type header
 */
private fun detectFormatFromContentType(headers: Map<String, List<String>>?): String {
    val contentType = headers?.get("Content-Type")?.firstOrNull()
        ?: headers?.get("content-type")?.firstOrNull()
        ?: return "TEXT"
    
    return when {
        contentType.contains("application/json", ignoreCase = true) -> "JSON"
        contentType.contains("application/xml", ignoreCase = true) || 
        contentType.contains("text/xml", ignoreCase = true) -> "XML"
        contentType.contains("text/html", ignoreCase = true) -> "HTML"
        contentType.contains("application/x-www-form-urlencoded", ignoreCase = true) -> "FORM"
        contentType.contains("multipart/form-data", ignoreCase = true) -> "MULTIPART"
        contentType.startsWith("text/", ignoreCase = true) -> "TEXT"
        contentType.contains("application/octet-stream", ignoreCase = true) -> "BINARY"
        else -> "TEXT"
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
