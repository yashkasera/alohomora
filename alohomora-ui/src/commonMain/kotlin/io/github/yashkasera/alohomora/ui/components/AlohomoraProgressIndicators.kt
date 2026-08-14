package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AlohomoraCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onBackground,
    strokeWidth: Dp = 4.dp,
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        strokeWidth = strokeWidth,
    )
}

@Preview
@Composable
private fun AlohomoraCircularProgressIndicatorPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlohomoraCircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}
