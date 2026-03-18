package io.github.yashkasera.alohomora.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
fun DashboardHeader(
    onConnectClick: () -> Unit,
    isConnected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = MaterialTheme.dimens.stroke.small, color = MaterialTheme.colorScheme.onSurface)
            .padding(MaterialTheme.dimens.margin.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface,
                        shape = RectangleShape
                    )
                    .border(MaterialTheme.dimens.stroke.small, MaterialTheme.colorScheme.onSurface, RectangleShape)
            )
            Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
            Text(
                text = "ALOHOMORA // UNIT 01",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        CanvasButton(
            text = if (isConnected) "DISCONNECT" else "CONNECT",
            onClick = onConnectClick,
            inverted = !isConnected
        )
    }
}

@Composable
fun CanvasButton(
    text: String,
    onClick: () -> Unit,
    inverted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val shape: Shape = RectangleShape
    val backgroundColor = if (inverted) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface
    val contentColor = if (inverted) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .border(MaterialTheme.dimens.stroke.small, MaterialTheme.colorScheme.onSurface, shape)
            .background(backgroundColor, shape)
            .padding(horizontal = MaterialTheme.dimens.margin.xxl, vertical = MaterialTheme.dimens.margin.sm)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor
        )
    }
}
