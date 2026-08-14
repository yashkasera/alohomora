package io.github.yashkasera.alohomora.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import androidx.compose.ui.tooling.preview.Preview

enum class TopBarLayout {
    CENTER_ALIGNED,
    START_ALIGNED,
}

/**
 * The title and subtitle carry test tags of their own. Every console screen renders this bar, so
 * one pair of tags covers all of them — and the subtitle is where several screens publish their
 * live counts ("12 REQUESTS"), which is the cheapest assertion a list test can make.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AlohomoraTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    layout: TopBarLayout = TopBarLayout.CENTER_ALIGNED,
    showDivider: Boolean = false,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    when (layout) {
        TopBarLayout.CENTER_ALIGNED -> CenterAlignedTopAppBar(
            modifier = modifier,
            navigationIcon = { navigationIcon?.invoke() },
            title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.TOP_BAR_TITLE),
                    )
                    subtitle.takeUnless { it.isNullOrBlank() }?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.tertiary,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.StartEllipsis,
                            modifier = Modifier.testTag(AlohomoraTestTags.Chrome.TOP_BAR_SUBTITLE),
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
            ),
            actions = actions,
        )
        TopBarLayout.START_ALIGNED -> Column(
            modifier = modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.testTag(AlohomoraTestTags.Chrome.TOP_BAR_TITLE),
                    )
                },
                subtitle = {
                    subtitle?.let {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.testTag(AlohomoraTestTags.Chrome.TOP_BAR_SUBTITLE),
                        )
                    }
                },
                contentPadding = PaddingValues(
                    vertical = MaterialTheme.dimens.margin.md,
                    horizontal = MaterialTheme.dimens.margin.xxl,
                ),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                actions = actions,
            )
            if (showDivider) {
                AlohomoraHorizontalDivider()
            }
        }
    }
}

@Preview
@Composable
private fun AlohomoraTopBarPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                AlohomoraTopBar(title = "Traffic", subtitle = "42 REQUESTS")
                Spacer(Modifier.height(8.dp))
                AlohomoraTopBar(
                    title = "Traces",
                    subtitle = "grouped by trace id",
                    layout = TopBarLayout.START_ALIGNED,
                    showDivider = true,
                )
            }
        }
    }
}
