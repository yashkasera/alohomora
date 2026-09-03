package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.plugin.InternalPlugin
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.components.NeedsAttentionPager
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.Activity
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.icons.SlidersHorizontal
import io.github.yashkasera.alohomora.ui.icons.ToggleLeft
import io.github.yashkasera.alohomora.ui.icons.Waypoints
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

internal data class OverviewModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isInverse: Boolean,
    val route: Routes,
)

/**
 * Grid key for a module card.
 *
 * Every plugin routes to [Routes.Extension], so keying on the route's class name alone gives
 * every plugin card the key `Extension` and `LazyVerticalGrid` throws on the duplicate as soon
 * as a second dashboard plugin is registered. Built-in routes are `data object`s so their
 * class names are already unique.
 */
internal val OverviewModule.gridKey: String
    get() = when (val route = route) {
        is Routes.Extension -> "Extension:${route.extensionId}"
        else -> route::class.simpleName.orEmpty()
    }

private fun CustomScreenPlugin.toOverviewModule() = OverviewModule(
    title = title,
    subtitle = description ?: "",
    // Never inverted. Emphasis is reserved for the first built-in module;
    // keying it off `priority` scattered white cards through the grid.
    isInverse = false,
    route = Routes.Extension(extensionId = id),
    icon = icon ?: Icons.Layers,
)

/**
 * Splits dashboard plugins into Alohomora's own ([InternalPlugin]) and consumer-registered ones,
 * which the grid renders under "SYSTEM MODULES" and "CUSTOM MODULES" respectively.
 *
 * The partition runs on the plugin, not on the mapped [OverviewModule]: every module routes to
 * [Routes.Extension], so partitioning after the map put every plugin on the internal side and
 * left the "CUSTOM MODULES" branch permanently unreachable.
 */
internal fun partitionDashboardModules(
    plugins: List<CustomScreenPlugin>,
): Pair<List<OverviewModule>, List<OverviewModule>> {
    val (internal, custom) = plugins.partition { it is InternalPlugin }
    return internal.map { it.toOverviewModule() } to custom.map { it.toOverviewModule() }
}

internal val builtInModules = listOf(
    OverviewModule(
        title = "Traffic",
        subtitle = "LIVE STREAM",
        icon = Icons.Route,
        isInverse = true,
        route = Routes.Traffic,
    ),
    OverviewModule(
        title = "Traces",
        subtitle = "SPANS & LATENCY",
        icon = Icons.Waypoints,
        isInverse = false,
        route = Routes.Traces,
    ),
    OverviewModule(
        title = "Database",
        subtitle = "INSPECTOR",
        icon = Icons.Database,
        isInverse = false,
        route = Routes.Database,
    ),
    OverviewModule(
        title = "Errors",
        subtitle = "CRITICAL LOGS",
        icon = Icons.AlertTriangle,
        isInverse = false,
        route = Routes.Error,
    ),
    OverviewModule(
        title = "Cache",
        subtitle = "KEY-VALUE STORE",
        icon = Icons.Key,
        isInverse = false,
        route = Routes.Cache,
    ),
    OverviewModule(
        title = "Events",
        subtitle = "SYSTEM TRIGGERS",
        icon = Icons.Activity,
        isInverse = true,
        route = Routes.Events,
    ),
    OverviewModule(
        title = "Feature Flags",
        subtitle = "FLAGS & CONFIG",
        icon = Icons.ToggleLeft,
        isInverse = false,
        route = Routes.FeatureFlags,
    ),
    OverviewModule(
        title = "Config",
        subtitle = "BUILD SETTINGS",
        icon = Icons.SlidersHorizontal,
        isInverse = false,
        route = Routes.Config,
    ),
    OverviewModule(
        title = "Git History",
        subtitle = "COMMIT HISTORY",
        icon = Icons.GitGraph,
        isInverse = false,
        route = Routes.GitHistory,
    ),
)

/** Live count for the module cards that surface one; null renders no badge. */
internal fun OverviewState.countFor(route: Routes): Long? = when (route) {
    Routes.Traffic -> trafficCount
    Routes.Error -> errorCount
    Routes.Events -> eventCount
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun OverviewScreen(
    onNavigate: (Routes) -> Unit,
) {
    val viewModel = koinViewModel<OverviewViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pluginCount = PluginRegistry.getAllPlugins().size
    val (internalPlugins, customPlugins) = remember(pluginCount) {
        partitionDashboardModules(PluginRegistry.getDashboardPlugins())
    }
    val defaultModules = remember(internalPlugins) {
        builtInModules + internalPlugins
    }

    val lazyGridState = rememberLazyGridState()

    Scaffold {
        LazyVerticalGrid(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .testTag(AlohomoraTestTags.Overview.GRID),
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            contentPadding = PaddingValues(MaterialTheme.dimens.margin.xl),
            state = lazyGridState,
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                OverviewHeader()
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("SERVER STATUS")
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                OverviewStatusCard(
                    state = state,
                    onToggle = { isEnabled ->
                        viewModel.onEvent(OverviewEvent.ToggleServer(isEnabled))
                    },
                    onPortChange = { port -> viewModel.onEvent(OverviewEvent.PortChanged(port)) },
                    onRememberDeviceChange = { remember ->
                        viewModel.onEvent(OverviewEvent.RememberDeviceChanged(remember))
                    },
                )
            }
            if (state.activeMockRuleCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MockRulesBanner(
                        ruleCount = state.activeMockRuleCount,
                        onClear = { viewModel.onEvent(OverviewEvent.ClearMockRules) },
                    )
                }
            }
            if (state.attentionItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
                        NeedsAttentionPager(
                            items = state.attentionItems,
                            onErrorClick = { error ->
                                onNavigate(Routes.ErrorDetails(error.id))
                            },
                            onTrafficClick = { traffic ->
                                onNavigate(Routes.TrafficDetails(traffic.id))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag(AlohomoraTestTags.Overview.NEEDS_ATTENTION),
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionHeader("SYSTEM MODULES")
            }
            items(
                items = defaultModules,
                key = { module -> module.gridKey },
                span = { module ->
                    if (module.isInverse) GridItemSpan(maxLineSpan)
                    else GridItemSpan(1)
                },
            ) { module ->
                ModuleCard(
                    module,
                    onNavigate = onNavigate,
                    count = state.countFor(module.route),
                    modifier = Modifier.testTag(
                        AlohomoraTestTags.Overview.moduleCard(module.gridKey),
                    ),
                )
            }
            customPlugins.ifEmpty { null }?.let { plugins ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("CUSTOM MODULES")
                }
                items(plugins, key = { plugin -> plugin.gridKey }) { module ->
                    ModuleCard(
                        module,
                        onNavigate = onNavigate,
                        modifier = Modifier.testTag(
                            AlohomoraTestTags.Overview.moduleCard(module.gridKey),
                        ),
                    )
                }
            }
            fabClearanceItem()
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column {
        Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
    }
}

@Preview
@Composable
private fun OverviewGridPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.dimens.margin.xl),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OverviewHeader()
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("SERVER STATUS")
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OverviewStatusCard(
                        state = OverviewState(
                            serverEnabled = true,
                            deviceConnectionStatus = DevConnectionStatus.Connected,
                            trafficCount = 128,
                            errorCount = 4,
                            eventCount = 42,
                        ),
                        onToggle = {},
                        onPortChange = {},
                        onRememberDeviceChange = {},
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MockRulesBanner(ruleCount = 2, onClear = {})
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader("SYSTEM MODULES")
                }
                items(
                    items = builtInModules,
                    key = { it.gridKey },
                    span = { module ->
                        if (module.isInverse) GridItemSpan(maxLineSpan)
                        else GridItemSpan(1)
                    },
                ) { module ->
                    ModuleCard(
                        module,
                        onNavigate = {},
                        count = OverviewState(
                            trafficCount = 128,
                            errorCount = 4,
                            eventCount = 42,
                        ).countFor(module.route),
                    )
                }
            }
        }
    }
}
