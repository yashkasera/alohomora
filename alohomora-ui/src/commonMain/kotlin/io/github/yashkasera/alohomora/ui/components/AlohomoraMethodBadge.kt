package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun MethodBadge(method: String, modifier: Modifier = Modifier) {
    val isWrite = method in listOf("POST", "PUT", "PATCH", "DELETE")

    val backgroundColor =
        if (isWrite) MaterialTheme.colorScheme.inverseSurface
        else Color.Transparent

    val contentColor = if (isWrite) MaterialTheme.colorScheme.inverseOnSurface
    else MaterialTheme.colorScheme.onSurface

    AlohomoraChip(
        label = method,
        modifier = modifier,
        uppercase = true,
        containerColor = backgroundColor,
        contentColor = contentColor,
        border = BorderStroke(
            width = MaterialTheme.dimens.stroke.small,
            color = contentColor,
        ).takeIf { !isWrite },
    )
}

@Preview
@Composable
private fun MethodBadgePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MethodBadge(method = "GET")
                MethodBadge(method = "POST")
                MethodBadge(method = "DELETE")
            }
        }
    }
}
