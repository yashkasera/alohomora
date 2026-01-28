package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.presentation.theme.CanvasBlack
import io.github.yashkasera.alohomora.presentation.theme.CanvasWhite
import io.github.yashkasera.alohomora.presentation.theme.CanvasDarkGray
// We'll reuse AlertRed if we really need it, or just use Black for active state.
import io.github.yashkasera.alohomora.presentation.theme.CanvasAlertRed


@Composable
fun DashboardHeader(
    onConnectClick: () -> Unit,
    isConnected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CanvasWhite)
            .border(width = 1.dp, color = CanvasBlack)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Branding/Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(if (isConnected) CanvasBlack else CanvasWhite, shape = RectangleShape)
                    .border(1.dp, CanvasBlack, RectangleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ALOHOMORA // UNIT 01",
                style = MaterialTheme.typography.titleMedium,
                color = CanvasBlack
            )
        }

        // Right: Actions
        CanvasButton(
            text = if (isConnected) "DISCONNECT" else "CONNECT",
            onClick = onConnectClick,
            // Invert colors for active state or simple difference
            inverted = !isConnected
            // If connected (Disconnect option), show as standard button.
            // If disconnected (Connect option), show as inverted (Call to action).
        )
    }
}

@Composable
fun CanvasButton(
    text: String,
    onClick: () -> Unit,
    inverted: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Brutalist: Sharp edges, thick border
    val shape: Shape = RectangleShape
    val backgroundColor = if (inverted) CanvasBlack else CanvasWhite
    val contentColor = if (inverted) CanvasWhite else CanvasBlack

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(1.dp, CanvasBlack, shape)
            .background(backgroundColor, shape)
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}
