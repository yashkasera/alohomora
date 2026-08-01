package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import io.github.yashkasera.alohomora.common.DateUtils
import io.github.yashkasera.alohomora.common.Event
import io.github.yashkasera.alohomora.ui.components.AlohomoraCodeBlock
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun LazyItemScope.EventItem(
    event: Event,
    showProperties: Boolean,
    onClick: () -> Unit,
) {
    Column(
        // Sunken once read, matching the traffic row. Events have no error state, so unlike
        // TrafficItem there is nothing that should override the dimming.
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (event.isViewed) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.Transparent
                },
            )
            .clickable(onClick = onClick)
            .animateItem()
            .padding(horizontal = MaterialTheme.dimens.margin.xl, vertical = MaterialTheme.dimens.margin.lg),
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
            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))

            AlohomoraCodeBlock(
                content = event.properties?.toString() ?: "{}",
                isScrollable = false
            )
        }
    }
}
