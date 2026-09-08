package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme

/**
 * The M3 Expressive morphing loader — a shape that cycles through the seven-polygon set rather than a
 * spinning arc.
 *
 * Defaults its colour to `onBackground` like every other wrapper (see the emphasis rule in
 * `DESIGN_SYSTEM.md`), not to the raw M3 `primary` the underlying component reaches for. Two overloads:
 * indeterminate (no argument) for "working, unknown duration", and determinate (a `progress` lambda)
 * for a known fraction. Prefer this over [AlohomoraCircularProgressIndicator] where the extra
 * expressiveness reads as intentional — connection states, long panel loads — and keep the plain
 * circular one for tight inline spots where a morphing shape would be noise.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlohomoraLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    LoadingIndicator(
        modifier = modifier,
        color = color,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlohomoraLoadingIndicator(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
) {
    LoadingIndicator(
        progress = progress,
        modifier = modifier,
        color = color,
    )
}

@Preview
@Composable
private fun AlohomoraLoadingIndicatorPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlohomoraLoadingIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}
