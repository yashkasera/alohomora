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
// There is deliberately no `monospaceValue` flag. It existed to opt ids and durations into a
// monospace face "where a proportional font makes two values hard to compare" — but `bodySmall` is
// already JetBrains Mono, so there was no proportional font to escape. All the flag did was swap the
// bundled face for `FontFamily.Monospace`, which resolves to a different font on Android, iOS and
// Desktop. Every caller passed `true`.
@Composable
fun KeyValueRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SectionLabel(label)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
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
