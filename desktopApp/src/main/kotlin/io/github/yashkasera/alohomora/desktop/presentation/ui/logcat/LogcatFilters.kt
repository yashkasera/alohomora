package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel
import io.github.yashkasera.alohomora.desktop.presentation.model.LogcatFilterState

@Composable
fun LogcatFilters(
    filterState: LogcatFilterState,
    availableTags: List<String>,
    onToggleLevel: (LogLevel) -> Unit,
    onSelectTag: (String?) -> Unit,
    onSearch: (String) -> Unit,
) {
    Column {
        Row {
            LogLevel.values().forEach { level ->
                val selected = filterState.enabledLevels.contains(level)
                val colors = if (selected) {
                    ButtonDefaults.buttonColors(
                        containerColor = levelColor(level),
                        contentColor = Color.White
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE0E0E0),
                        contentColor = Color.Black
                    )
                }
                Button(
                    onClick = { onToggleLevel(level) },
                    colors = colors,
                    contentPadding = ButtonDefaults.ContentPadding,
                ) {
                    Text(level.shortName)
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagDropdown(
                selectedTag = filterState.selectedTag,
                availableTags = availableTags,
                onSelectTag = onSelectTag,
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = filterState.searchQuery,
                onValueChange = onSearch,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> Color(0xFF9E9E9E)
    LogLevel.DEBUG -> Color(0xFF1976D2)
    LogLevel.INFO -> Color(0xFF2E7D32)
    LogLevel.WARN -> Color(0xFFEF6C00)
    LogLevel.ERROR -> Color(0xFFC62828)
    LogLevel.FATAL -> Color(0xFF9C27B0)
}
