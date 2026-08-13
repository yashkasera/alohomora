package io.github.yashkasera.alohomora.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
fun AppTheme(
    isDarkState: MutableState<Boolean>? = null,
    initialIsDark: Boolean = isSystemInDarkTheme(),
    themeId: String = "default",
    content: @Composable () -> Unit,
) {
    val ownedState = remember { mutableStateOf(initialIsDark) }
    val effectiveState = isDarkState ?: ownedState
    val isDark by effectiveState
    val theme = AlohomoraThemes.forId(themeId, isDark)

    CompositionLocalProvider(
        LocalThemeIsDark provides effectiveState,
        LocalAlohomoraDimens provides AlohomoraDimensInstance,
        LocalAlohomoraColors provides theme,
    ) {
        MaterialTheme(
            colorScheme = theme.materialColorScheme,
            typography = AlohomoraTypography(),
            shapes = AlohomoraShapes,
            content = content,
        )
    }
}

/**
 * Hoisted because [LocalAlohomoraDimens] is a `staticCompositionLocalOf`, which does not compare the
 * values it is handed — a fresh [AlohomoraDimens] identity on each recomposition invalidates the whole
 * subtree under the theme. The dimens are constant, so one instance is all there ever needs to be.
 *
 * [AlohomoraTypography] gets no such treatment: it is `@Composable` (it resolves bundled `Font`s) and
 * so cannot be hoisted or `remember`ed here. The resource loader caches the faces itself.
 */
private val AlohomoraDimensInstance = AlohomoraDimens()

val LocalThemeIsDark = compositionLocalOf { mutableStateOf(true) }
