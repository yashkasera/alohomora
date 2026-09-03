package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

object AlohomoraCardDefaults {
    val shape @Composable get() = MaterialTheme.shapes.small

    @Composable
    fun colors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor: Color = contentColorFor(containerColor),
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = contentColor,
    )

    val border
        @Composable get() = BorderStroke(
            MaterialTheme.dimens.stroke.small,
            MaterialTheme.colorScheme.outlineVariant,
        )
}

@Composable
fun AlohomoraCard(
    modifier: Modifier = Modifier,
    shape: Shape = AlohomoraCardDefaults.shape,
    colors: CardColors = AlohomoraCardDefaults.colors(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        content = content,
    )
}

@Composable
fun AlohomoraCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AlohomoraCardDefaults.shape,
    colors: CardColors = AlohomoraCardDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun AlohomoraOutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = AlohomoraCardDefaults.shape,
    colors: CardColors = AlohomoraCardDefaults.colors(),
    border: BorderStroke = AlohomoraCardDefaults.border,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(
        modifier = modifier,
        shape = shape,
        colors = colors,
        border = border,
        content = content,
    )
}

@Stable
data class ViewedStateColors(
    val containerColor: State<Color>,
    val titleColor: State<Color>,
)

@Composable
fun rememberViewedStateColors(
    isViewed: Boolean,
    unviewedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    viewedContainerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    unviewedTitleColor: Color = MaterialTheme.colorScheme.onSurface,
    viewedTitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
): ViewedStateColors = ViewedStateColors(
    containerColor = animateColorAsState(
        if (isViewed) viewedContainerColor else unviewedContainerColor,
        spring(),
    ),
    titleColor = animateColorAsState(
        if (isViewed) viewedTitleColor else unviewedTitleColor,
        spring(),
    ),
)

@Preview
@Composable
private fun AlohomoraCardsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AlohomoraCard {
                    Text("Filled card", modifier = Modifier.padding(16.dp))
                }
                AlohomoraCard(onClick = {}) {
                    Text("Clickable card", modifier = Modifier.padding(16.dp))
                }
                AlohomoraOutlinedCard {
                    Text("Outlined card", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}
