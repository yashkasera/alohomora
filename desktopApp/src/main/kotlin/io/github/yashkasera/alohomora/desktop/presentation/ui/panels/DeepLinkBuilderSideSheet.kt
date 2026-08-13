package io.github.yashkasera.alohomora.desktop.presentation.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.data.local.DeepLinkEntry
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.AlohomoraSideSheet
import io.github.yashkasera.alohomora.ui.components.EmptyState
import io.github.yashkasera.alohomora.desktop.presentation.ui.components.SectionLabel
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraPrimaryTabRow
import io.github.yashkasera.alohomora.ui.components.AlohomoraTab
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Clock
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Link
import io.github.yashkasera.alohomora.ui.icons.Play
import io.github.yashkasera.alohomora.ui.icons.Plus
import io.github.yashkasera.alohomora.ui.icons.Trash
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.dimens

private val TABS = listOf("Builder", "History")

@Composable
fun DeepLinkBuilderSideSheet(
    visible: Boolean,
    initialUrl: String,
    history: List<DeepLinkEntry>,
    onOpen: (String) -> Unit,
    onRemoveHistoryEntry: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val parsed = remember(initialUrl) { parseUrl(initialUrl) }
    var scheme by remember(initialUrl) { mutableStateOf(parsed.scheme) }
    var customScheme by remember(initialUrl) { mutableStateOf(parsed.customScheme) }
    var showSchemeDropdown by remember { mutableStateOf(false) }
    var host by remember(initialUrl) { mutableStateOf(parsed.host) }
    var port by remember(initialUrl) { mutableStateOf(parsed.port) }
    var path by remember(initialUrl) { mutableStateOf(parsed.path) }
    val queryParams = remember(initialUrl) { mutableStateListOf(*parsed.queryParams.toTypedArray()) }
    var fragment by remember(initialUrl) { mutableStateOf(parsed.fragment) }

    val composedUrl by remember {
        derivedStateOf {
            buildUrl(
                scheme = if (scheme == CUSTOM_SCHEME) customScheme else scheme,
                host = host,
                port = port,
                path = path,
                queryParams = queryParams,
                fragment = fragment,
            )
        }
    }

    fun loadUrl(url: String) {
        val p = parseUrl(url)
        scheme = p.scheme
        customScheme = p.customScheme
        host = p.host
        port = p.port
        path = p.path
        queryParams.clear()
        queryParams.addAll(p.queryParams)
        fragment = p.fragment
        selectedTab = 0
    }

    AlohomoraSideSheet(
        visible = visible,
        onDismiss = onDismiss,
        widthFraction = 0.4f,
        header = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = MaterialTheme.dimens.margin.xl,
                        vertical = MaterialTheme.dimens.margin.md,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Deep Links",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                AlohomoraIconButton(onClick = onDismiss) {
                    Icon(Icons.X, contentDescription = "Close")
                }
            }
            AlohomoraPrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TABS.forEachIndexed { index, tab ->
                    AlohomoraTab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = tab,
                        uppercase = false,
                    )
                }
            }
        },
    ) {
        when (selectedTab) {
            0 -> BuilderTab(
                scheme = scheme,
                customScheme = customScheme,
                showSchemeDropdown = showSchemeDropdown,
                host = host,
                port = port,
                path = path,
                queryParams = queryParams,
                fragment = fragment,
                composedUrl = composedUrl,
                onSchemeChange = { scheme = it },
                onCustomSchemeChange = { customScheme = it },
                onShowSchemeDropdown = { showSchemeDropdown = it },
                onHostChange = { host = it },
                onPortChange = { port = it },
                onPathChange = { path = it },
                onFragmentChange = { fragment = it },
                onOpen = onOpen,
                onReset = {
                    scheme = "https"
                    customScheme = ""
                    host = ""
                    port = ""
                    path = ""
                    queryParams.clear()
                    fragment = ""
                },
            )

            1 -> HistoryTab(
                history = history,
                onUse = ::loadUrl,
                onReplay = onOpen,
                onRemove = onRemoveHistoryEntry,
                onClearAll = onClearHistory,
            )
        }
    }
}

@Composable
private fun BuilderTab(
    scheme: String,
    customScheme: String,
    showSchemeDropdown: Boolean,
    host: String,
    port: String,
    path: String,
    queryParams: MutableList<QueryParam>,
    fragment: String,
    composedUrl: String,
    onSchemeChange: (String) -> Unit,
    onCustomSchemeChange: (String) -> Unit,
    onShowSchemeDropdown: (Boolean) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onPathChange: (String) -> Unit,
    onFragmentChange: (String) -> Unit,
    onOpen: (String) -> Unit,
    onReset: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = MaterialTheme.dimens.margin.xl,
                vertical = MaterialTheme.dimens.margin.md,
            ),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
    ) {
        SectionLabel("Preview")
        SelectionContainer {
            AlohomoraCodeBlock(
                content = composedUrl.ifBlank { "scheme://host/path" },
                isScrollable = false,
            )
        }

        AlohomoraHorizontalDivider()

        SectionLabel("Scheme")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AlohomoraTextButton(
                text = if (scheme == CUSTOM_SCHEME) "Custom" else scheme,
                onClick = { onShowSchemeDropdown(true) },
                size = AlohomoraButtonSize.SMALL,
                uppercase = false,
                trailingIcon = {
                    Icon(
                        Icons.ChevronDown,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.dimens.margin.sm),
                    )
                },
            )
            AlohomoraDropdownMenu(
                expanded = showSchemeDropdown,
                onDismissRequest = { onShowSchemeDropdown(false) },
            ) {
                SCHEME_OPTIONS.forEach { option ->
                    AlohomoraDropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSchemeChange(option)
                            onShowSchemeDropdown(false)
                        },
                    )
                }
            }
            if (scheme == CUSTOM_SCHEME) {
                AlohomoraTextField(
                    value = customScheme,
                    onValueChange = onCustomSchemeChange,
                    placeholder = "myapp",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        SectionLabel("Host")
        AlohomoraTextField(
            value = host,
            onValueChange = onHostChange,
            placeholder = "example.com",
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Port")
        AlohomoraTextField(
            value = port,
            onValueChange = { onPortChange(it.filter { c -> c.isDigit() }.take(5)) },
            placeholder = "Optional",
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Path")
        AlohomoraTextField(
            value = path,
            onValueChange = onPathChange,
            placeholder = "/api/users/123",
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel("Query parameters")
            AlohomoraIconButton(
                onClick = { queryParams.add(QueryParam("", "")) },
            ) {
                Icon(
                    Icons.Plus,
                    contentDescription = "Add parameter",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
            }
        }

        queryParams.forEachIndexed { index, param ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AlohomoraTextField(
                    value = param.key,
                    onValueChange = { queryParams[index] = param.copy(key = it) },
                    placeholder = "key",
                    modifier = Modifier.weight(1f),
                )
                AlohomoraTextField(
                    value = param.value,
                    onValueChange = { queryParams[index] = param.copy(value = it) },
                    placeholder = "value",
                    modifier = Modifier.weight(1f),
                )
                AlohomoraIconButton(
                    onClick = { queryParams.removeAt(index) },
                ) {
                    Icon(
                        Icons.Trash,
                        contentDescription = "Remove",
                        modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        SectionLabel("Fragment")
        AlohomoraTextField(
            value = fragment,
            onValueChange = onFragmentChange,
            placeholder = "section",
            modifier = Modifier.fillMaxWidth(),
        )

        AlohomoraHorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
        ) {
            AlohomoraFilledButton(
                text = "Open on device",
                onClick = { onOpen(composedUrl) },
                enabled = composedUrl.isNotBlank(),
                modifier = Modifier.weight(1f),
                size = AlohomoraButtonSize.MEDIUM,
                uppercase = false,
            )
            AlohomoraOutlinedButton(
                text = "Reset",
                onClick = onReset,
            )
        }
    }
}

@Composable
private fun HistoryTab(
    history: List<DeepLinkEntry>,
    onUse: (String) -> Unit,
    onReplay: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    if (history.isEmpty()) {
        EmptyState(
            icon = Icons.Clock,
            title = "No history yet",
            subtitle = "Deep links you open will appear here.",
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xl,
                    vertical = MaterialTheme.dimens.margin.sm,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${history.size} entries",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            AlohomoraTextButton(
                text = "Clear all",
                onClick = onClearAll,
                uppercase = false,
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
        AlohomoraHorizontalDivider()
        history.forEach { entry ->
            DeepLinkHistoryRow(
                entry = entry,
                onUse = { onUse(entry.url) },
                onReplay = { onReplay(entry.url) },
                onRemove = { onRemove(entry.url) },
            )
        }
    }
}

@Composable
private fun DeepLinkHistoryRow(
    entry: DeepLinkEntry,
    onUse: () -> Unit,
    onReplay: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onUse)
                .padding(
                    horizontal = MaterialTheme.dimens.margin.xl,
                    vertical = MaterialTheme.dimens.margin.sm,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Link,
                contentDescription = null,
                modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(MaterialTheme.dimens.margin.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.url,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatRelativeTime(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AlohomoraIconButton(onClick = onReplay) {
                Icon(
                    Icons.Play,
                    contentDescription = "Open on device",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                )
            }
            AlohomoraIconButton(onClick = onRemove) {
                Icon(
                    Icons.Trash,
                    contentDescription = "Remove",
                    modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    AlohomoraHorizontalDivider()
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    return when {
        seconds < 60 -> "just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        seconds < 86400 -> "${seconds / 3600}h ago"
        else -> "${seconds / 86400}d ago"
    }
}

data class QueryParam(val key: String, val value: String)

private const val CUSTOM_SCHEME = "Custom..."
private val SCHEME_OPTIONS = listOf("https", "http", "deeplink", "content", CUSTOM_SCHEME)

private data class ParsedUrl(
    val scheme: String,
    val customScheme: String,
    val host: String,
    val port: String,
    val path: String,
    val queryParams: List<QueryParam>,
    val fragment: String,
)

private fun parseUrl(raw: String): ParsedUrl {
    if (raw.isBlank()) return ParsedUrl("https", "", "", "", "", emptyList(), "")

    val schemeEnd = raw.indexOf("://")
    if (schemeEnd == -1) return ParsedUrl("https", "", raw, "", "", emptyList(), "")

    val rawScheme = raw.substring(0, schemeEnd)
    val scheme: String
    val customScheme: String
    if (rawScheme in listOf("https", "http", "deeplink", "content")) {
        scheme = rawScheme
        customScheme = ""
    } else {
        scheme = CUSTOM_SCHEME
        customScheme = rawScheme
    }

    val afterScheme = raw.substring(schemeEnd + 3)

    val fragmentIdx = afterScheme.indexOf('#')
    val fragment: String
    val beforeFragment: String
    if (fragmentIdx >= 0) {
        fragment = afterScheme.substring(fragmentIdx + 1)
        beforeFragment = afterScheme.substring(0, fragmentIdx)
    } else {
        fragment = ""
        beforeFragment = afterScheme
    }

    val queryIdx = beforeFragment.indexOf('?')
    val queryParams: List<QueryParam>
    val beforeQuery: String
    if (queryIdx >= 0) {
        beforeQuery = beforeFragment.substring(0, queryIdx)
        val queryString = beforeFragment.substring(queryIdx + 1)
        queryParams = queryString.split('&').filter { it.isNotBlank() }.map { pair ->
            val eqIdx = pair.indexOf('=')
            if (eqIdx >= 0) QueryParam(pair.substring(0, eqIdx), pair.substring(eqIdx + 1))
            else QueryParam(pair, "")
        }
    } else {
        beforeQuery = beforeFragment
        queryParams = emptyList()
    }

    val pathIdx = beforeQuery.indexOf('/')
    val host: String
    val port: String
    val path: String
    if (pathIdx >= 0) {
        val hostPort = beforeQuery.substring(0, pathIdx)
        path = beforeQuery.substring(pathIdx)
        val colonIdx = hostPort.indexOf(':')
        if (colonIdx >= 0) {
            host = hostPort.substring(0, colonIdx)
            port = hostPort.substring(colonIdx + 1)
        } else {
            host = hostPort
            port = ""
        }
    } else {
        val colonIdx = beforeQuery.indexOf(':')
        if (colonIdx >= 0) {
            host = beforeQuery.substring(0, colonIdx)
            port = beforeQuery.substring(colonIdx + 1)
        } else {
            host = beforeQuery
            port = ""
        }
        path = ""
    }

    return ParsedUrl(scheme, customScheme, host, port, path, queryParams, fragment)
}

private fun buildUrl(
    scheme: String,
    host: String,
    port: String,
    path: String,
    queryParams: List<QueryParam>,
    fragment: String,
): String {
    if (scheme.isBlank() && host.isBlank()) return ""

    return buildString {
        if (scheme.isNotBlank()) {
            append(scheme)
            append("://")
        }
        append(host)
        if (port.isNotBlank()) {
            append(':')
            append(port)
        }
        if (path.isNotBlank()) {
            if (!path.startsWith('/')) append('/')
            append(path)
        }
        val validParams = queryParams.filter { it.key.isNotBlank() }
        if (validParams.isNotEmpty()) {
            append('?')
            append(validParams.joinToString("&") { "${it.key}=${it.value}" })
        }
        if (fragment.isNotBlank()) {
            append('#')
            append(fragment)
        }
    }
}
