package io.github.yashkasera.alohomora.presentation.ui

import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import org.koin.compose.KoinIsolatedContext

@Composable
internal fun AlohomoraApp(
    startDestination: Routes = Routes.Overview,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
    /**
     * Dismisses the whole console. Null on hosts that provide their own way out (Android's
     * system back button); non-null when embedded in something modal, such as an iOS sheet.
     */
    onClose: (() -> Unit)? = null,
) {
    // KoinIsolatedContext, not KoinContext: KoinContext reads Koin's GlobalContext, which
    // Alohomora deliberately never populates so it can coexist with the host app's Koin.
    val koinApplication = Alohomora.koinApplication
    if (koinApplication == null) {
        AlohomoraTheme { Text("Alohomora is not initialized. Call Alohomora.init() first.") }
        return
    }
    KoinIsolatedContext(koinApplication) {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null
        ) {
            AlohomoraTheme {
                onThemeChanged(LocalThemeIsDark.current.value)
                AlohomoraNavHost(startDestination = startDestination, onClose = onClose)
            }
        }
    }
}
