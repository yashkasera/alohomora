package io.github.yashkasera.alohomora.desktop.presentation.ui.logcat

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonSize
import io.github.yashkasera.alohomora.ui.components.AlohomoraFilledButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraOutlinedButton
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun LogcatControls(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
) {

    if (running) {
        AlohomoraFilledButton(
            text = "Stop",
            size = AlohomoraButtonSize.SMALL,
            containerColor = MaterialTheme.colorScheme.error,
            onClick = onStop,
        )
    } else {
        AlohomoraOutlinedButton(
            text = "Start",
            onClick = onStart,
            size = AlohomoraButtonSize.SMALL,
        )
    }
    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
    AlohomoraOutlinedButton(
        text = "Clear",
        size = AlohomoraButtonSize.SMALL,
        onClick = onClear,
    )
}
