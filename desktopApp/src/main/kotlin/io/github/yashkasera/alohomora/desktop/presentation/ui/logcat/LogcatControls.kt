package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton

@Composable
fun LogcatControls(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (running) {
            AlohomoraFilledButton(text = "Stop", onClick = onStop)
        } else {
            AlohomoraFilledButton(text = "Start", onClick = onStart)
        }
        AlohomoraFilledButton(text = "Clear", onClick = onClear)
    }
}
