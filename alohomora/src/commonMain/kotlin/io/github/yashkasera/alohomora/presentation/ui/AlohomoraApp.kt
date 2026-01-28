package io.github.yashkasera.alohomora.presentation.ui

import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.yashkasera.alohomora.presentation.theme.AlohomoraTheme
import org.koin.compose.KoinContext

@Composable
internal fun AlohomoraApp(onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}) {
    KoinContext {
        CompositionLocalProvider(
            LocalRippleConfiguration provides null
        ) {
            AlohomoraTheme(onThemeChanged = onThemeChanged) {
                AlohomoraNavHost()
            }
        }
    }
}
