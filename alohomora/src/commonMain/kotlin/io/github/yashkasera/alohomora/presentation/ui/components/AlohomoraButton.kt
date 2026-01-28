package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.presentation.ui.components.AlohomoraButtonSize.LARGE
import io.github.yashkasera.alohomora.presentation.ui.components.AlohomoraButtonSize.MEDIUM
import io.github.yashkasera.alohomora.presentation.ui.components.AlohomoraButtonSize.SMALL

internal enum class AlohomoraButtonSize {
    SMALL,
    MEDIUM,
    LARGE;

}

@Composable
internal fun AlohomoraButton(
    modifier: Modifier = Modifier,
    text: String,
    isFilled: Boolean = true,
    size: AlohomoraButtonSize = AlohomoraButtonSize.MEDIUM,
    onClick: () -> Unit,
) {
    val containerColor = if (isFilled) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isFilled) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isFilled) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val colors = ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    Button(
        modifier = modifier,
        onClick = onClick,
        shape = RectangleShape,
        colors = colors,
        border = BorderStroke(1.dp, borderColor).takeIf { !isFilled },
        content = {
            Text(
                text = text.uppercase(),
                style = when (size) {
                    SMALL -> MaterialTheme.typography.labelSmall
                    MEDIUM -> MaterialTheme.typography.labelMedium
                    LARGE -> MaterialTheme.typography.labelLarge
                },
                fontWeight = FontWeight.Bold
            )
        }
    )
}

