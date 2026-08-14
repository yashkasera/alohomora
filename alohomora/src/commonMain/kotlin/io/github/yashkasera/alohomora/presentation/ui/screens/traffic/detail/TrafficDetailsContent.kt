package io.github.yashkasera.alohomora.presentation.ui.screens.traffic.detail

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TrafficEntry
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.jsonviewer.JsonTreeView
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Clock
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.RefreshCw
import io.github.yashkasera.alohomora.ui.icons.Search
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.theme.dimens
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TrafficDetailsContent(
    modifier: Modifier = Modifier,
    trace: TrafficEntry,
    onCopy: (String) -> Unit = {},
) {
    val tabs = listOf("OVERVIEW", "REQUEST", "RESPONSE")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val overviewScrollState = rememberScrollState()
    val requestScrollState = rememberScrollState()
    val responseListState = rememberLazyListState()

    val currentPage by remember { derivedStateOf { pagerState.currentPage } }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
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
                            .padding(horizontal = MaterialTheme.dimens.margin.md)
                            .padding(top = MaterialTheme.dimens.margin.md),
                    ) {
                        OverviewTab(entry = trace)
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(requestScrollState)
                            .padding(horizontal = MaterialTheme.dimens.margin.md)
                            .padding(top = MaterialTheme.dimens.margin.md),
                    ) {
                        RequestTab(trace = trace, onCopy = onCopy)
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

@Composable
private fun OverviewTab(entry: TrafficEntry) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AlohomoraChip(
                label = entry.method?.uppercase().orEmpty(),
            )
            if (entry.mockedBy != null) {
                AlohomoraChip(
                    label = "Mocked",
                    uppercase = false,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
        Text(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(MaterialTheme.dimens.margin.sm),
            text = entry.pathWithQuery(),
            style = MaterialTheme.typography.labelLarge,
        )
    }

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

    val requestSize = entry.requestSize ?: 0
    val responseSize = entry.responseSize ?: 0L

    HeroStatsSection(
        statusCode = entry.status ?: 0,
        latencyMs = entry.duration ?: 0,
        responseSize = responseSize,
        requestSize = requestSize,
        format = entry.responseContentType ?: "*",
    )

    AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.xxxl))

    InfoRowsSection(trace = entry)

    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.huge))
}

@Composable
private fun RequestTab(trace: TrafficEntry, onCopy: (String) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AlohomoraChip(
                label = trace.method?.uppercase().orEmpty(),
            )
            if (trace.mockedBy != null) {
                AlohomoraChip(
                    label = "Mocked",
                    uppercase = false,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                )
            }
        }
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
        Text(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(MaterialTheme.dimens.margin.sm),
            text = trace.url.orEmpty(),
            style = MaterialTheme.typography.labelLarge,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

        // Request headers section with formatted display
        trace.requestHeaders?.ifEmpty { null }?.let { headers ->
            val formattedHeaders = formatHeaders(headers)
            val headerCount = headers.values.sumOf { it.size }

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(title = "REQUEST HEADERS")
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                AlohomoraChip(label = headerCount.toString())
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(MaterialTheme.dimens.margin.lg),
            ) {
                formattedHeaders.forEach { headerLine ->
                    Text(
                        text = headerLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))
        }

        // Only rendered when there is a body. A GET has none, and showing an empty block plus
        // a badge reading "*" (the subtype of a wildcard content type) made a normal request
        // look like a capture failure.
        val requestBody = trace.requestBody?.takeIf { it.isNotBlank() }
        if (requestBody != null) {
            val requestFormat = detectFormatFromContentType(trace.requestContentType)
            val subtype = requestFormat.contentSubtype.uppercase()

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionHeader(title = "REQUEST BODY")
                // Omitted for a wildcard content type; "*" told the user nothing.
                if (subtype != "*") {
                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                    AlohomoraChip(
                        label = subtype,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
            AlohomoraCodeBlock(
                content = requestBody,
                isScrollable = false,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
        }

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth()) {
            if (trace.requestBody.isNullOrEmpty().not()) {
                AlohomoraOutlinedButton(
                    text = "Copy JSON",
                    onClick = {
                        onCopy(requestBody ?: trace.responseBody ?: "")
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
            }
            AlohomoraOutlinedButton(
                text = "Copy cURL",
                onClick = { onCopy(trace.curlCommand()) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.huge))
    }
}

@Composable
private fun ResponseTab(
    trace: TrafficEntry,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val responseSize = trace.responseSize ?: 0L
    val responseFormat = detectFormatFromContentType(trace.responseContentType)

    Column(
        modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
    ) {
        ResponseMetadata(
            statusCode = trace.status ?: 0,
            format = responseFormat,
            sizeBytes = responseSize,
        )
        trace.responseHeaders?.let { headers ->
            val formattedHeaders = formatHeaders(headers)
            val headerCount = headers.values.sumOf { it.size }
            var expanded by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = MaterialTheme.dimens.margin.lg)
                    .clickable {
                        expanded = !expanded
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(
                    modifier = Modifier.weight(1f),
                    title = "RESPONSE HEADERS",
                )
                AlohomoraChip(label = headerCount.toString())
                Icon(
                    imageVector = Icons.ChevronDown,
                    contentDescription = "Expand",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }
            if (expanded) {
                AlohomoraCodeBlock(
                    isScrollable = false,
                    accentBorder = !trace.isSuccessful(),
                    content = formattedHeaders.joinToString("\n"),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xl))
            }
        }

        JsonTreeView(
            json = trace.responseBody ?: "{}",
            listState = listState,
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
        Text(
            text = "STATUS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = HttpStatusCode.fromValue(statusCode).toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.sm),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = "LATENCY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = latencyMs.toString(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.xs))
                    Text(
                        text = "ms",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "SIZE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
                Text(
                    text = formatBytes(responseSize),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = "(${formatBytes(requestSize)} up)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = "FORMAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                Text(
                    text = detectFormatFromContentType(format).contentSubtype,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun InfoRowsSection(trace: TrafficEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xxl)) {
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
        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.lg))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xs))
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
private fun ResponseMetadata(
    statusCode: Int,
    format: ContentType,
    sizeBytes: Long,
) {
    Row(
        modifier = Modifier
            .padding(vertical = MaterialTheme.dimens.margin.md)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
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
    runCatching {
        ContentType.parse(contentType ?: throw Exception())
    }.getOrDefault(ContentType.Any)
