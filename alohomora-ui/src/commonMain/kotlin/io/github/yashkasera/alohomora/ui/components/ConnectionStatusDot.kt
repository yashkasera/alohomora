package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.CanvasAlertRed
import io.github.yashkasera.alohomora.ui.theme.CanvasSuccessGreen
import io.github.yashkasera.alohomora.ui.theme.logError
import io.github.yashkasera.alohomora.ui.theme.success
import io.github.yashkasera.alohomora.ui.theme.warning

enum class ConnectionDotState {
    Connected,
    Disconnected,
    Reconnecting,
}

@Composable
fun ConnectionStatusDot(
    state: ConnectionDotState,
    size: Dp = 10.dp,
) {
    val color = when (state) {
        ConnectionDotState.Connected -> MaterialTheme.colorScheme.success
        ConnectionDotState.Disconnected -> MaterialTheme.colorScheme.logError
        ConnectionDotState.Reconnecting -> MaterialTheme.colorScheme.warning
    }

    Box {
        if (state == ConnectionDotState.Connected) {
            val transition = rememberInfiniteTransition(label = "connection-dot")
            val pulseScale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "pulse-scale",
            )
            val pulseAlpha by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        easing = LinearOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "pulse-alpha",
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .background(color, CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(size)
                .background(color, CircleShape),
        )
    }
}
