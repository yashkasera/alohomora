package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors

enum class ConnectionDotState {
    Connected,
    Disconnected,
    Reconnecting,
}

/**
 * Note for UI tests: the [ConnectionDotState.Connected] branch runs an `infiniteRepeatable` pulse,
 * and an infinite Compose animation never lets the test clock go idle — `waitForIdle()` would hang.
 * Tests that need to observe a connected console must drive `mainClock` manually with
 * `autoAdvance = false`. The other two states are static and safe.
 */
@Composable
fun ConnectionStatusDot(
    state: ConnectionDotState,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp,
) {
    val color = when (state) {
        ConnectionDotState.Connected -> MaterialTheme.alohomoraColors.success
        ConnectionDotState.Disconnected -> MaterialTheme.colorScheme.error
        ConnectionDotState.Reconnecting -> MaterialTheme.alohomoraColors.warning
    }

    Box(modifier = modifier) {
        if (state == ConnectionDotState.Connected) {
            val transition = rememberInfiniteTransition(label = "connection-dot")
            val pulseScale by transition.animateFloat(
                initialValue = 1f,
                targetValue = 2.5f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500,
                        easing = LinearOutSlowInEasing,
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
                        easing = LinearOutSlowInEasing,
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

@Preview
@Composable
private fun ConnectionStatusDotPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Row(
                modifier = Modifier.padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ConnectionStatusDot(state = ConnectionDotState.Connected)
                ConnectionStatusDot(state = ConnectionDotState.Reconnecting)
                ConnectionStatusDot(state = ConnectionDotState.Disconnected)
            }
        }
    }
}
