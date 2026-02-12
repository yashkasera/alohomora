package io.github.yashkasera.alohomora.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberTopAppBarState as materialRememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Modifier

object AlohomoraTopAppBarDefaults {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun rememberTopAppBarState(): TopAppBarState = materialRememberTopAppBarState()

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    fun enterAlwaysScrollBehavior(state: TopAppBarState): TopAppBarScrollBehavior =
        TopAppBarDefaults.enterAlwaysScrollBehavior(state)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlohomoraLargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    LargeTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
    )
}
