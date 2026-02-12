package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.desktop.domain.model.DevToolsConnection

@Composable
fun StatusPill(state: DevToolsConnection) {
    val (text, _) = when (state) {
        DevToolsConnection.Disconnected -> "DISCONNECTED" to Color(0xFF8A8A8A)
        is DevToolsConnection.Connecting -> "CONNECTING" to Color(0xFFB27A00)
        is DevToolsConnection.Connected -> "CONNECTED" to Color(0xFF2E7D32)
        is DevToolsConnection.Failed -> "FAILED" to Color(0xFFC62828)
    }
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RectangleShape)
            .border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer, RectangleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
