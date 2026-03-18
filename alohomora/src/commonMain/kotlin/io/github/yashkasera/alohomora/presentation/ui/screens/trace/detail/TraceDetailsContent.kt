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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TraceEntry
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.icons.Download
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Clock
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TraceDetailsContent(
    modifier: Modifier = Modifier,
    trace: TraceEntry,
) {
    val tabs = listOf("OVERVIEW", "REQUEST", "RESPONSE")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Hoist scroll states to persist across tab switches
    val overviewScrollState = rememberScrollState()
    val requestScrollState = rememberScrollState()
    val responseListState = rememberLazyListState()

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
            when (page) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(overviewScrollState)
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp),
                    ) {
                        OverviewTab(trace = trace)
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(requestScrollState)
                            .padding(horizontal = 24.dp)
                            .padding(top = 24.dp),
                    ) {
                        RequestTab(trace = trace)
                    }
                }

                2 -> {
                    ResponseTab(
                        trace = trace,
                        listState = responseListState,
                        modifier = Modifier.fillMaxSize(),
                    )
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
        url = trace.pathWithQuery(),
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Calculate sizes from content
    val requestSize = trace.requestSize ?: 0
    val responseSize = trace.responseSize ?: 0L

    // Detect format from Content-Type header
    val responseFormat = detectFormatFromContentType(trace.responseContentType)

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
        val requestFormat = detectFormatFromContentType(trace.requestContentType)

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionHeader(title = "REQUEST BODY")
            Spacer(modifier = Modifier.width(8.dp))
            FormatBadge(format = requestFormat.contentSubtype.uppercase())
        }

        Spacer(modifier = Modifier.height(16.dp))
        AlohomoraCodeBlock(
            content = trace.requestBody ?: "{}",
            isScrollable = false,
            modifier = Modifier.fillMaxWidth(),
        )

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
private fun ResponseTab(
    trace: TraceEntry,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    // Calculate size and detect format
    val responseSize = trace.responseSize ?: 0L
    val responseFormat = detectFormatFromContentType(trace.responseContentType)

    JsonTreeView(
        json = trace.responseBody ?: "{}",
        listState,
        parentContent = {
            item {
                ResponseMetadata(
                    statusCode = trace.status ?: 0,
                    format = responseFormat,
                    sizeBytes = responseSize,
                )
            }
            trace.responseHeaders?.let { headers ->
                val formattedHeaders = formatHeaders(headers)
                val headerCount = headers.values.sumOf { it.size }
                item(
                    key = "response_headers",
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(
                                horizontal = 24.dp,
                                vertical = 16.dp,
                            )
                            .clickable {
                                expanded = !expanded
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionHeader(
                            modifier = Modifier.weight(1f),
                            title = "RESPONSE HEADERS",
                        )
                        CountBadge(count = headerCount)
                        Icon(
                            imageVector = Icons.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    if (expanded) {
                        AlohomoraCodeBlock(
                            modifier = Modifier.animateItem(),
                            isScrollable = false,
                            accentBorder = !trace.isSuccessful(),
                            content = formattedHeaders.joinToString("\n"),
                        )
                    }
                }
            }
        },
    )
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
    format: ContentType,
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
                    text = format.contentSubtype,
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
            icon = Icons.Clock,
            label = "TIMESTAMP",
            value = DateUtils.format(trace.time ?: 0, DateUtils.Format.ISO_DATE_TIME),
        )

        InfoRow(
            icon = Icons.Server,
            label = "HOST",
            value = trace.host,
        )

        InfoRow(
            icon = Icons.RefreshCw,
            label = "SCHEME",
            value = trace.scheme?.uppercase() ?: "HTTPS",
        )

        InfoRow(
            icon = Icons.RefreshCw,
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
private fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
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
    format: ContentType,
    sizeBytes: Long,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
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
                    text = HttpStatusCode.fromValue(statusCode).toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = format.contentSubtype.uppercase(),
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

/**
 * Formats bytes to human-readable format (B, KB, MB, GB)
 */
private fun formatBytes(bytes: Long): String {
    if (bytes < 0) return "0 B"
    if (bytes == 0L) return "0 B"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val unitIndex = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        .coerceAtMost(units.size - 1)

    val value = bytes / 1024.0.pow(unitIndex)

    return when {
        unitIndex == 0 -> "$bytes ${units[unitIndex]}"
        value >= 100 -> "${value.toInt()} ${units[unitIndex]}"
        value >= 10 -> "${round(value * 10) / 10} ${units[unitIndex]}"
        else -> "${round(value * 100) / 100} ${units[unitIndex]}"
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
private fun detectFormatFromContentType(contentType: String?): ContentType =
    ContentType.parse(contentType ?: "")
