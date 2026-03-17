package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton

@Composable
fun LogcatControls(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {

    if (running) {
        AlohomoraFilledButton(text = "Stop", onClick = onStop)
    } else {
        AlohomoraFilledButton(text = "Start", onClick = onStart)
    }
    Spacer(modifier = Modifier.width(8.dp))
    AlohomoraFilledButton(text = "Clear", onClick = onClear)
}
