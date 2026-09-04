package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens

@Composable
internal fun MockRulesBanner(
    ruleCount: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warningColor = MaterialTheme.alohomoraColors.warning

    AlohomoraCard(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = AlohomoraCardDefaults.colors(
            containerColor = MaterialTheme.alohomoraColors.warningContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.lg,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(warningColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AlertTriangle,
                    contentDescription = null,
                    tint = warningColor,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$ruleCount mock rules active",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Intercepting requests",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AlohomoraTextButton(
                text = "Clear",
                onClick = onClear,
                uppercase = false,
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview
@Composable
private fun MockRulesBannerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xl)) {
                MockRulesBanner(
                    ruleCount = 3,
                    onClear = {},
                )
            }
        }
    }
}
