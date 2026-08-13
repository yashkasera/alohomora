package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.yashkasera.alohomora.ui.theme.dimens

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
