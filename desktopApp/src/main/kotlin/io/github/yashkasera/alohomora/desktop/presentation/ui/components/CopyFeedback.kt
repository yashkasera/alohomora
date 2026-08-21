package io.github.yashkasera.alohomora.desktop.presentation.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

val LocalCopyFeedback = staticCompositionLocalOf<(String) -> Unit> { {} }
