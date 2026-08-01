package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Contents of the sheet shown when a desktop client asks to connect.
 *
 * Split out from any particular presentation because the two platforms host it differently:
 * Android overlays it on the current Activity (or falls back to a notification when there is no
 * foreground Activity to overlay), while iOS presents it on the key window. Only the host differs
 * — what the user reads is identical.
 */
@Composable
internal fun ConnectionRequestSheetContent(
    otp: String,
    onRememberChange: (Boolean) -> Unit,
    /**
     * Sizing is the host's decision, not this composable's.
     *
     * iOS presents inside a fixed-height detent, so the content must fill it — a wrap-height
     * card left the controller's black background showing below, which read as the sheet
     * floating. Android's ModalBottomSheet sizes itself to its content, so it wants wrap.
     */
    modifier: Modifier = Modifier,
) {
    // Unchecked by default. Persisting a credential is an explicit choice — inferring it from a
    // successful pairing is exactly what this checkbox exists to stop.
    var remember by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(MaterialTheme.dimens.margin.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Connection request",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
        Text(
            text = "A desktop client wants to inspect this app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))
        Text(
            text = otp,
            // Deliberately oversized: this is read off the phone and typed on a laptop, often at
            // arm's length on a desk.
            fontSize = 48.sp,
            style = MaterialTheme.typography.displayMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

        Text(
            text = "Enter this code on the desktop client",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                // The whole row toggles, not just the box: a checkbox alone is a small target
                // on a phone held in one hand.
                .clickable {
                    remember = !remember
                    onRememberChange(remember)
                },
            // Top, not centre: the label wraps to two lines on a narrow screen and a centred
            // checkbox then floats between them.
            verticalAlignment = Alignment.Top,
        ) {
            Checkbox(
                checked = remember,
                onCheckedChange = { checked ->
                    remember = checked
                    onRememberChange(checked)
                },
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            Column {
                Text(
                    text = "Remember this computer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "Skip the code next time",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}
