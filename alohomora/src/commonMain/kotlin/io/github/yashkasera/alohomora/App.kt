package io.github.yashkasera.alohomora

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.generated.resources.*
import io.github.yashkasera.alohomora.generated.resources.Res
import io.github.yashkasera.alohomora.generated.resources.cyclone
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.LocalThemeIsDark
import io.github.yashkasera.alohomora.ui.components.AlohomoraButtonDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraElevatedButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@Preview
@Composable
fun App(onThemeChanged: @Composable (isDark: Boolean) -> Unit = {}) = AppTheme(onThemeChanged) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.cyclone),
            style = MaterialTheme.typography.displayLarge
        )

        var isRotating by remember { mutableStateOf(false) }

        val rotate = remember { Animatable(0f) }
        val target = 360f
        if (isRotating) {
            LaunchedEffect(Unit) {
                while (isActive) {
                    val remaining = (target - rotate.value) / target
                    rotate.animateTo(target, animationSpec = tween((1_000 * remaining).toInt(), easing = LinearEasing))
                    rotate.snapTo(0f)
                }
            }
        }

        Image(
            modifier = Modifier
                .size(250.dp)
                .padding(16.dp)
                .run { rotate(rotate.value) },
            imageVector = vectorResource(Res.drawable.ic_cyclone),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            contentDescription = null
        )

        AlohomoraElevatedButton(
            text = stringResource(if (isRotating) Res.string.stop else Res.string.run),
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .widthIn(min = 200.dp),
            onClick = { isRotating = !isRotating },
            content = {
                Icon(vectorResource(Res.drawable.ic_rotate_right), contentDescription = null)
                Spacer(Modifier.size(AlohomoraButtonDefaults.iconSpacing))
                Text(
                    stringResource(if (isRotating) Res.string.stop else Res.string.run)
                )
            }
        )

        var isDark by LocalThemeIsDark.current
        val icon = remember(isDark) {
            if (isDark) Res.drawable.ic_light_mode
            else Res.drawable.ic_dark_mode
        }

        AlohomoraElevatedButton(
            text = stringResource(Res.string.theme),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 200.dp),
            onClick = { isDark = !isDark },
            content = {
                Icon(vectorResource(icon), contentDescription = null)
                Spacer(Modifier.size(AlohomoraButtonDefaults.iconSpacing))
                Text(stringResource(Res.string.theme))
            }
        )

        val uriHandler = LocalUriHandler.current
        AlohomoraTextButton(
            text = stringResource(Res.string.open_github),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(min = 200.dp),
            onClick = { uriHandler.openUri("https://github.com/terrakok") },
        )
    }
}
