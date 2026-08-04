package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Label on the left, value on the right — the metadata row shape both detail sheets use.
 *
 * Extracted from `TrafficPanel`, where it was private, rather than copied into the Traces sheet: two
 * near-identical key-value rows drift, and then the two sheets read differently for no reason.
 */
@Composable
fun KeyValueRow(label: String, value: String, monospaceValue: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            // Opt-in, for ids and durations, where a proportional font makes two values hard to
            // compare at a glance.
            fontFamily = if (monospaceValue) FontFamily.Monospace else null,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = MaterialTheme.dimens.margin.md),
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
