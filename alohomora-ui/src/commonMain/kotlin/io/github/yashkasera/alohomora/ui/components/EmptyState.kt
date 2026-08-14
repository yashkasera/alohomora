package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * Explains why a surface has nothing on it.
 *
 * @param action optional control rendered below the text — a retry or refresh, for the cases
 *   where the user can do something about the emptiness.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    // Tagged here rather than at each of the ten call sites: "did this screen fall back to its
    // empty state" is the same assertion everywhere, and only the copy differs.
    Box(
        modifier = Modifier.fillMaxSize().testTag(AlohomoraTestTags.Chrome.EMPTY_STATE).then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = MaterialTheme.dimens.margin.xxxl),
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
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontStyle = FontStyle.Italic,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(AlohomoraTestTags.Chrome.EMPTY_STATE_TITLE),
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        letterSpacing = 0.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (action != null) {
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))
                action()
            }
        }
    }
}
