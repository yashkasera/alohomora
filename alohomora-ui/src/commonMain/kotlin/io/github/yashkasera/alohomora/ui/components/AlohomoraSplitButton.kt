package io.github.yashkasera.alohomora.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import io.github.yashkasera.alohomora.ui.icons.ChevronDown
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens

/**
 * A primary action welded to a toggle that opens more — "replay" next to "replay with edits", "save"
 * next to "save as". The two halves share one split container and morph at the seam when either is
 * pressed, which is the M3 Expressive read a plain button-plus-icon can't give.
 *
 * [onClick] fires the primary action; [checked]/[onCheckedChange] drive the trailing toggle (wire it
 * to whatever menu or sheet the secondary options live in). The default trailing glyph is a chevron
 * that flips as [checked] changes; pass [trailingContent] to override. Keep the leading label short —
 * the leading half is the thing users reach for.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlohomoraSplitButton(
    onClick: () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingContent: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable RowScope.() -> Unit = {
        val rotation by animateFloatAsState(
            targetValue = if (checked) 180f else 0f,
            label = "splitButtonChevron",
        )
        Icon(
            imageVector = Icons.ChevronDown,
            contentDescription = null,
            modifier = Modifier
                .size(SplitButtonDefaults.TrailingIconSize)
                .rotate(rotation),
        )
    },
) {
    SplitButtonLayout(
        leadingButton = {
            SplitButtonDefaults.LeadingButton(onClick = onClick, content = leadingContent)
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                content = trailingContent,
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun AlohomoraSplitButtonPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            var checked by remember { mutableStateOf(false) }
            AlohomoraSplitButton(
                onClick = {},
                checked = checked,
                onCheckedChange = { checked = it },
                leadingContent = { Text("Replay") },
                modifier = Modifier.padding(MaterialTheme.dimens.margin.lg),
            )
        }
    }
}
