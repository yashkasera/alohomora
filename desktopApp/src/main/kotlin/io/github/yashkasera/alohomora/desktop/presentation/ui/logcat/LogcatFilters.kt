package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel
import io.github.yashkasera.alohomora.desktop.presentation.model.LogcatFilterState
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField

@Composable
fun LogcatFilters(
    filterState: LogcatFilterState,
    onToggleLevel: (LogLevel) -> Unit,
    onSelectTag: (String?) -> Unit,
    onPackageChange: (String) -> Unit,
    onSearch: (String) -> Unit,
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
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AlohomoraTextField(
                value = filterState.selectedTag.orEmpty(),
                onValueChange = { onSelectTag(it.ifBlank { null }) },
                placeholder = "Tag",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            AlohomoraTextField(
                value = filterState.packageName,
                onValueChange = onPackageChange,
                placeholder = "Package name",
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        AlohomoraTextField(
            value = filterState.searchQuery,
            onValueChange = onSearch,
            placeholder = "Search",
            singleLine = true,
            modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
        )
    }
}

