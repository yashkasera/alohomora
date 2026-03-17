package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.TelemetryEvent
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock

@Composable
fun LazyItemScope.TelemetryItem(
    event: TelemetryEvent,
    showProperties: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateItem()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Header Row: Title + Timestamp
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Text(
                text = DateUtils.format(event.time, DateUtils.Format.HH_MM_SS),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }

        // Code Block - only shown if showProperties is true
        if (showProperties) {
            Spacer(modifier = Modifier.height(8.dp))

            AlohomoraCodeBlock(
                content = event.properties?.toString() ?: "{}",
                isScrollable = false
            )
        }
    }
}
