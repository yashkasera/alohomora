package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import io.github.yashkasera.alohomora.ui.theme.logDebug
import io.github.yashkasera.alohomora.ui.theme.logError
import io.github.yashkasera.alohomora.ui.theme.logFatal
import io.github.yashkasera.alohomora.ui.theme.logInfo
import io.github.yashkasera.alohomora.ui.theme.logVerbose
import io.github.yashkasera.alohomora.ui.theme.logWarn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.LogEntry
import io.github.yashkasera.alohomora.desktop.domain.model.LogLevel

@Composable
fun LogcatRow(entry: LogEntry) {
    val color = levelColor(entry.level)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = entry.timestamp,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(140.dp),
        )
        Text(
            text = "${entry.pid}:${entry.tid}",
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp),
        )
        Text(
            text = entry.level.shortName,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = entry.tag,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(160.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = entry.message,
            color = color,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
@ReadOnlyComposable
internal fun levelColor(level: LogLevel): Color = when (level) {
    LogLevel.VERBOSE -> MaterialTheme.colorScheme.logVerbose
    LogLevel.DEBUG -> MaterialTheme.colorScheme.logDebug
    LogLevel.INFO -> MaterialTheme.colorScheme.logInfo
    LogLevel.WARN -> MaterialTheme.colorScheme.logWarn
    LogLevel.ERROR -> MaterialTheme.colorScheme.logError
    LogLevel.FATAL -> MaterialTheme.colorScheme.logFatal
}
