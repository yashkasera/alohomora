package io.github.yashkasera.alohomora.presentation.ui

import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.theme.AlohomoraTheme
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import org.koin.compose.KoinContext

@Composable
internal fun AlohomoraApp(
    startDestination: Routes = Routes.Overview,
    onThemeChanged: @Composable (isDark: Boolean) -> Unit = {},
) {
    KoinContext {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null
        ) {
            AlohomoraTheme {
                onThemeChanged(LocalThemeIsDark.current.value)
                AlohomoraNavHost(startDestination = startDestination)
            }
        }
    }
}
