package io.github.yashkasera.alohomora.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.yashkasera.alohomora.ui.theme.AppTheme

object AlohomoraTabDefaults {
    const val uppercase: Boolean = true
}

@Composable
fun AlohomoraPrimaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    tabs: @Composable () -> Unit,
) {
    PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tabs = tabs,
    )
}

@Composable
fun AlohomoraTab(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    uppercase: Boolean = AlohomoraTabDefaults.uppercase,
    icon: (@Composable (() -> Unit))? = null,
) {
    val label = if (uppercase) text.uppercase() else text
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        text = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
        icon = icon,
    )
}

@Preview
@Composable
private fun AlohomoraTabsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            AlohomoraPrimaryTabRow(selectedTabIndex = 0) {
                AlohomoraTab(selected = true, onClick = {}, text = "Builder")
                AlohomoraTab(selected = false, onClick = {}, text = "History")
            }
        }
    }
}
