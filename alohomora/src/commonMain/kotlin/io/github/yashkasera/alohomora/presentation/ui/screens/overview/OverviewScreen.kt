package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.AlohomoraImpl
import io.github.yashkasera.alohomora.plugin.CustomScreenPlugin
import io.github.yashkasera.alohomora.plugin.InternalPlugin
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.components.AlohomoraCard
import io.github.yashkasera.alohomora.ui.components.AlohomoraCardDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.components.NeedsAttentionPager
import io.github.yashkasera.alohomora.ui.components.fabClearanceItem
import io.github.yashkasera.alohomora.ui.icons.Activity
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Key
import io.github.yashkasera.alohomora.ui.icons.Layers
import io.github.yashkasera.alohomora.ui.icons.Route
import io.github.yashkasera.alohomora.ui.icons.SlidersHorizontal
import io.github.yashkasera.alohomora.ui.icons.Tag
import io.github.yashkasera.alohomora.ui.icons.ToggleLeft
import io.github.yashkasera.alohomora.ui.icons.ToggleRight
import io.github.yashkasera.alohomora.ui.icons.Waypoints
import io.github.yashkasera.alohomora.ui.testing.AlohomoraTestTags
import io.github.yashkasera.alohomora.ui.theme.AppTheme
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
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

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(MaterialTheme.dimens.margin.md)
                    .padding(top = MaterialTheme.dimens.margin.md),
            ) {
                Row(
                    modifier = Modifier
                        .padding(top = MaterialTheme.dimens.margin.xl),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Icon(
                        imageVector = Icons.AlohomoraFull,
                        modifier = Modifier
                            .width(148.dp),
                        contentDescription = null,
                    )
                }
                AlohomoraImpl.config?.let {
                    Row(
                        modifier = Modifier
                            .padding(top = MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        it.appName?.let { appName ->
                            Text(
                                modifier = Modifier.weight(1f),
                                text = appName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xs),
                        ) {
                            Icon(
                                imageVector = Icons.Tag,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(MaterialTheme.dimens.icon.sm),
                                contentDescription = null,
                            )
                            Text(
                                text = "v${it.versionName}",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        },
    ) {
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
                Column {
                    Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
                    Text(
                        "SERVER STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    DevToolsStatusCard(
                        state = state,
                        onToggle = { isEnabled ->
                            viewModel.onEvent(
                                OverviewEvent.ToggleServer(
                                    isEnabled,
                                ),
                            )
                        },
                        onPortChange = { port -> viewModel.onEvent(OverviewEvent.PortChanged(port)) },
                    )
                }
            }
            if (state.activeMockRuleCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MockRulesActiveBanner(
                        ruleCount = state.activeMockRuleCount,
                        onClear = { viewModel.onEvent(OverviewEvent.ClearMockRules) },
                    )
                }
            }
            if (state.attentionItems.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.xxl))
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
                Column {
                    Spacer(Modifier.height(MaterialTheme.dimens.margin.xxl))
                    Text(
                        "SYSTEM MODULES",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            items(
                items = defaultModules,
                key = { module -> module.gridKey },
                span = { module ->
                    if (module.isInverse) GridItemSpan(maxLineSpan)
                    else GridItemSpan(1)
                },
            ) { modules ->
                ModuleCard(
                    modules,
                    onNavigate = onNavigate,
                    modifier = Modifier.testTag(
                        AlohomoraTestTags.Overview.moduleCard(modules.gridKey),
                    ),
                )
            }
            customPlugins.ifEmpty { null }?.let { plugins ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.xxl))
                        Text(
                            "CUSTOM MODULES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.lg),
                        )
                    }
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
private fun DevToolsStatusCard(
    state: OverviewState,
    onToggle: (Boolean) -> Unit,
    onPortChange: (String) -> Unit,
) {
    val isConnected = state.deviceConnectionStatus == DevConnectionStatus.Connected
    val containerColor = if (isConnected)
        MaterialTheme.alohomoraColors.successContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    val toggleTint = if (state.serverEnabled)
        MaterialTheme.alohomoraColors.success
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    AlohomoraCard(
        onClick = {
            onToggle.invoke(!state.serverEnabled)
        },
        modifier = Modifier.testTag(AlohomoraTestTags.Overview.STATUS_CARD),
        shape = MaterialTheme.shapes.extraLarge,
        colors = AlohomoraCardDefaults.colors(
            containerColor = containerColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
        ) {
            val dotState = when (state.deviceConnectionStatus) {
                DevConnectionStatus.Connected -> ConnectionDotState.Connected
                DevConnectionStatus.AwaitingAuth -> ConnectionDotState.Reconnecting
                DevConnectionStatus.Disconnected,
                DevConnectionStatus.Off,
                    -> ConnectionDotState.Disconnected
            }
            ConnectionStatusDot(
                state = dotState,
                modifier = Modifier.testTag(AlohomoraTestTags.Overview.STATUS_DOT),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (state.deviceConnectionStatus) {
                        DevConnectionStatus.Connected -> "Connected"
                        DevConnectionStatus.AwaitingAuth -> "Awaiting code"
                        DevConnectionStatus.Disconnected -> "Waiting"
                        DevConnectionStatus.Off -> "Server off"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (state.serverEnabled) {
                    Text(
                        text = "Port ${state.serverPort}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!state.serverEnabled) {
                AlohomoraTextField(
                    label = "PORT",
                    value = state.serverPort,
                    onValueChange = onPortChange,
                    singleLine = true,
                    modifier = Modifier
                        .width(100.dp)
                        .testTag(AlohomoraTestTags.Overview.STATUS_PORT_FIELD),
                )
            }
            Icon(
                imageVector = if (state.serverEnabled)
                    Icons.ToggleRight
                else Icons.ToggleLeft,
                contentDescription = null,
                tint = toggleTint,
                modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
            )
        }
    }
}

@Composable
private fun ModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (overviewModule.isInverse) {
        FeaturedModuleCard(overviewModule, onNavigate, modifier)
    } else {
        StandardModuleCard(overviewModule, onNavigate, modifier)
    }
}

@Composable
private fun FeaturedModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEvents = overviewModule.route == Routes.Events
    val containerColor = if (isEvents)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isEvents)
        MaterialTheme.colorScheme.onTertiaryContainer
    else
        MaterialTheme.colorScheme.onPrimaryContainer

    AlohomoraCard(
        onClick = { onNavigate(overviewModule.route) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = AlohomoraCardDefaults.colors(containerColor = containerColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = MaterialTheme.dimens.margin.lg,
                    vertical = MaterialTheme.dimens.margin.xxl,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.margin.huge)
                    .background(
                        contentColor.copy(alpha = 0.12f),
                        CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = overviewModule.icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.standard),
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
            Text(
                text = overviewModule.title,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
            )
            Text(
                text = overviewModule.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun StandardModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconTint = when (overviewModule.route) {
        Routes.Error -> MaterialTheme.colorScheme.error
        Routes.Traces, Routes.FeatureFlags, Routes.GitHistory -> MaterialTheme.colorScheme.tertiary
        Routes.Database, Routes.Config -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }

    AlohomoraCard(
        onClick = { onNavigate(overviewModule.route) },
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = AlohomoraCardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(MaterialTheme.dimens.icon.xl)
                    .background(iconTint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = overviewModule.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(MaterialTheme.dimens.icon.lg),
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.margin.md))
            Text(
                text = overviewModule.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = overviewModule.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MockRulesActiveBanner(
    ruleCount: Int,
    onClear: () -> Unit,
) {
    val warningColor = MaterialTheme.alohomoraColors.warning

    AlohomoraCard(
        shape = MaterialTheme.shapes.extraLarge,
        colors = AlohomoraCardDefaults.colors(
            containerColor = warningColor.copy(alpha = 0.12f),
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
private fun FeaturedModuleCardsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                FeaturedModuleCard(
                    overviewModule = OverviewModule(
                        title = "Traffic",
                        subtitle = "LIVE STREAM",
                        icon = Icons.Route,
                        isInverse = true,
                        route = Routes.Traffic,
                    ),
                    onNavigate = {},
                )
                FeaturedModuleCard(
                    overviewModule = OverviewModule(
                        title = "Events",
                        subtitle = "SYSTEM TRIGGERS",
                        icon = Icons.Activity,
                        isInverse = true,
                        route = Routes.Events,
                    ),
                    onNavigate = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun StandardModuleCardsPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                items(
                    items = builtInModules.filter { !it.isInverse },
                    key = { it.gridKey },
                ) { module ->
                    StandardModuleCard(
                        overviewModule = module,
                        onNavigate = {},
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DevToolsStatusCardPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.padding(MaterialTheme.dimens.margin.xl),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            ) {
                DevToolsStatusCard(
                    state = OverviewState(
                        serverEnabled = true,
                        deviceConnectionStatus = DevConnectionStatus.Connected,
                    ),
                    onToggle = {},
                    onPortChange = {},
                )
                DevToolsStatusCard(
                    state = OverviewState(
                        serverEnabled = false,
                        deviceConnectionStatus = DevConnectionStatus.Off,
                    ),
                    onToggle = {},
                    onPortChange = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun MockRulesActiveBannerPreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.padding(MaterialTheme.dimens.margin.xl)) {
                MockRulesActiveBanner(
                    ruleCount = 3,
                    onClear = {},
                )
            }
        }
    }
}

@Preview
@Composable
private fun OverviewGridPreview() {
    AppTheme() {
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
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
                        Text(
                            "SERVER STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    DevToolsStatusCard(
                        state = OverviewState(
                            serverEnabled = true,
                            deviceConnectionStatus = DevConnectionStatus.Connected,
                        ),
                        onToggle = {},
                        onPortChange = {},
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MockRulesActiveBanner(ruleCount = 2, onClear = {})
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.xxl))
                        Text(
                            "SYSTEM MODULES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
                items(
                    items = builtInModules,
                    key = { it.gridKey },
                    span = { module ->
                        if (module.isInverse) GridItemSpan(maxLineSpan)
                        else GridItemSpan(1)
                    },
                ) { module ->
                    ModuleCard(module, onNavigate = {})
                }
            }
        }
    }
}

