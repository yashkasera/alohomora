package io.github.yashkasera.alohomora.ui.components

import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.yashkasera.alohomora.ui.theme.AppTheme

/**
 * A group of related actions that share one connected container and press-morph together — the M3
 * Expressive answer to a row of loose buttons.
 *
 * Thin pass-through: the underlying [ButtonGroup] already carries the expressive spacing and the
 * press-expansion, so there is nothing to re-skin. Populate it through the [ButtonGroupScope] the
 * `content` lambda hands you — `clickableItem`, `toggleableItem`, `customItem` — which keeps each
 * child a real button that grows when pressed while its neighbours give way. Note `clickableItem`
 * takes its composable positionally (third argument, before the defaulted `weight`/`enabled`), not as
 * a trailing lambda. Use this for action clusters that read as one control (a side-sheet header's
 * related actions, a toolbar segment); use [AlohomoraSingleChoiceToggleGroup] instead when the group
 * is a single-select filter.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlohomoraButtonGroup(
    modifier: Modifier = Modifier,
    content: @Composable ButtonGroupScope.() -> Unit,
) {
    ButtonGroup(
        modifier = modifier,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
private fun AlohomoraButtonGroupPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlohomoraButtonGroup {
                clickableItem({}, "One", { Text("One") })
                clickableItem({}, "Two", { Text("Two") })
                clickableItem({}, "Three", { Text("Three") })
            }
        }
    }
}
