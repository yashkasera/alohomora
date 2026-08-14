package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.launch

internal data class ClipboardCopyState(
    val copy: (text: String) -> Unit,
    val snackbarHostState: SnackbarHostState,
)

@Suppress("DEPRECATION")
@Composable
internal fun rememberClipboardCopy(): ClipboardCopyState {
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return remember(clipboard, snackbarHostState, scope) {
        ClipboardCopyState(
            copy = { text ->
                clipboard.setText(AnnotatedString(text))
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(
                        "Copied to clipboard",
                        duration = SnackbarDuration.Short,
                    )
                }
            },
            snackbarHostState = snackbarHostState,
        )
    }
}
