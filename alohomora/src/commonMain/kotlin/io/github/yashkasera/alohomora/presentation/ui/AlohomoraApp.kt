package io.github.yashkasera.alohomora.presentation.ui

import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.presentation.navigation.DestinationRequest
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import org.koin.compose.KoinIsolatedContext

@Composable
internal fun AlohomoraApp(
    destination: DestinationRequest = DestinationRequest.Root,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    /**
     * Dismisses the whole console, and the trigger for rendering a close button at all. Null on
     * hosts with their own way out (Android's system back); non-null when the console is presented
     * modally on iOS, where the Compose content swallows the sheet's swipe-to-dismiss.
     */
    onClose: (() -> Unit)? = null,
) {
    // KoinIsolatedContext, not KoinContext: KoinContext reads Koin's GlobalContext, which
    // Alohomora deliberately never populates so it can coexist with the host app's Koin.
    val koinApplication = AlohomoraImpl.koinApplication
    if (koinApplication == null) {
        AppTheme { Text("Alohomora is not initialized. Call Alohomora.init() first.") }
        return
    }
    KoinIsolatedContext(koinApplication) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null,
        ) {
            AppTheme {
                onThemeChanged(LocalThemeIsDark.current.value)
                AlohomoraNavHost(destination = destination, onClose = onClose)
            }
        }
    }
}
