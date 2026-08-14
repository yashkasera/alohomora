package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Explains why a surface has nothing on it.
 *
 * @param setup optional code snippet shown below the subtitle in a monospace block, so the
 *   reader knows which API call to wire up.
 * @param action optional control rendered below the text — a retry or refresh, for the cases
 *   where the user can do something about the emptiness.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    setup: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    // Tagged here rather than at each of the ten call sites: "did this screen fall back to its
    // empty state" is the same assertion everywhere, and only the copy differs.
    Box(
        modifier = Modifier.fillMaxSize().testTag(AlohomoraTestTags.Chrome.EMPTY_STATE)
            .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .padding(horizontal = MaterialTheme.dimens.margin.xxxl),
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.illustration)
                    .border(
                        width = MaterialTheme.dimens.stroke.medium,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.xl),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(AlohomoraTestTags.Chrome.EMPTY_STATE_TITLE),
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (setup != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.lg))
                AlohomoraCodeBlock(
                    content = setup,
                    isScrollable = false,
                )
            }

            if (action != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))
                action()
            }
        }
    }
}

@Preview
@Composable
private fun EmptyStatePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                EmptyState(
                    icon = Icons.Database,
                    title = "No tables yet",
                    subtitle = "Captured database state will appear here",
                    action = { AlohomoraFilledButton(text = "Refresh", onClick = {}) },
                )
            }
        }
    }
}
