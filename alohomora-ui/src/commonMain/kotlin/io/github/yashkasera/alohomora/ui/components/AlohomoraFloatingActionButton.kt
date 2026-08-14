package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Plus
import io.github.yashkasera.alohomora.ui.theme.AppTheme

@Composable
fun AlohomoraFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.background,
    shape: Shape? = null,
    content: @Composable () -> Unit,
) {
    val resolvedShape = shape ?: MaterialTheme.shapes.extraLarge
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = resolvedShape,
        content = content,
    )
}

@Composable
fun AlohomoraExtendedFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.onBackground,
    contentColor: Color = MaterialTheme.colorScheme.background,
    shape: Shape? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val resolvedShape = shape ?: MaterialTheme.shapes.large
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = resolvedShape,
        content = content,
    )
}

@Preview
@Composable
private fun AlohomoraFloatingActionButtonPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AlohomoraFloatingActionButton(onClick = {}) {
                    Icon(Icons.Plus, contentDescription = "Add")
                }
                AlohomoraExtendedFloatingActionButton(onClick = {}) {
                    Icon(Icons.Plus, contentDescription = null)
                    Text("New rule", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
