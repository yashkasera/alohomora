package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel
import io.github.yashkasera.alohomora.desktop.presentation.model.LogcatFilterState
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenu
import io.github.yashkasera.alohomora.ui.components.AlohomoraDropdownMenuItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilterChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun LogcatFilters(
    filterState: LogcatFilterState,
    availableTags: List<String>,
    onToggleLevel: (LogLevel) -> Unit,
    onTagFilterChange: (String) -> Unit,
    onPackageChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onToggleRegex: () -> Unit,
    searchFocusTrigger: Long = 0L,
) {
    val searchFocus = remember { FocusRequester() }
    LaunchedEffect(searchFocusTrigger) {
        if (searchFocusTrigger > 0) searchFocus.requestFocus()
    }
    Column {
        Row {
            LogLevel.entries.forEach { level ->
                val selected = filterState.enabledLevels.contains(level)
                val containerColor = if (selected) {
                    levelColor(level)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val contentColor = if (selected)
                    MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.inversePrimary
                AlohomoraFilledButton(
                    text = level.shortName,
                    onClick = { onToggleLevel(level) },
                    containerColor = containerColor,
                    contentColor = contentColor,
                    size = AlohomoraButtonSize.SMALL,
                    uppercase = false,
                )
                Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            }
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagFilterField(
                tagFilter = filterState.tagFilter,
                availableTags = availableTags,
                onTagFilterChange = onTagFilterChange,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.md))
            AlohomoraTextField(
                value = filterState.packageName,
                onValueChange = onPackageChange,
                placeholder = "Package name",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlohomoraTextField(
                value = filterState.searchQuery,
                onValueChange = onSearch,
                placeholder = "Search (e.g. crash | -noise)",
                singleLine = true,
                isError = filterState.isRegex && filterState.searchQuery.isNotBlank() &&
                    runCatching { Regex(filterState.searchQuery) }.isFailure,
                modifier = Modifier.weight(1f).focusRequester(searchFocus),
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            AlohomoraFilterChip(
                label = ".*",
                selected = filterState.isRegex,
                onClick = onToggleRegex,
                uppercase = false,
            )
        }
    }
}

@Composable
private fun TagFilterField(
    tagFilter: String,
    availableTags: List<String>,
    onTagFilterChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tagFocused by remember { mutableStateOf(false) }
    val currentToken = tagFilter.substringAfterLast("|").trim()
    val suggestions = if (tagFocused && currentToken.isNotEmpty()) {
        availableTags.filter { it.contains(currentToken, ignoreCase = true) }.take(10)
    } else emptyList()

    Box(modifier = modifier) {
        AlohomoraTextField(
            value = tagFilter,
            onValueChange = onTagFilterChange,
            placeholder = "Tag (e.g. OkHttp | -GmsCore)",
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { tagFocused = it.isFocused },
        )
        AlohomoraDropdownMenu(
            expanded = suggestions.isNotEmpty(),
            onDismissRequest = { tagFocused = false },
            modifier = Modifier.heightIn(max = 200.dp),
            properties = PopupProperties(focusable = false),
        ) {
            suggestions.forEach { tag ->
                AlohomoraDropdownMenuItem(
                    text = { Text(tag, style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        val prefix = tagFilter.substringBeforeLast("|", "")
                        val newValue = if (prefix.isNotBlank()) "$prefix | $tag" else tag
                        onTagFilterChange(newValue)
                        tagFocused = false
                    },
                )
            }
        }
    }
}
