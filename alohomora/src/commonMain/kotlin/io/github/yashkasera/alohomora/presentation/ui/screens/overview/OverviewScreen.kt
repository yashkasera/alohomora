package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.yashkasera.alohomora.Alohomora
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.components.CanvasBackground
import io.github.yashkasera.alohomora.common.AttentionItem
import io.github.yashkasera.alohomora.ui.components.AlohomoraChip
import io.github.yashkasera.alohomora.ui.components.AlohomoraChipDefaults
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.components.NeedsAttentionPager
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import io.github.yashkasera.alohomora.ui.icons.Activity
import io.github.yashkasera.alohomora.ui.icons.AlertTriangle
import io.github.yashkasera.alohomora.ui.icons.AlohomoraFull
import io.github.yashkasera.alohomora.ui.icons.ArrowRight
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
import io.github.yashkasera.alohomora.ui.icons.X
import io.github.yashkasera.alohomora.ui.theme.alohomoraColors
import io.github.yashkasera.alohomora.ui.theme.dimens
import org.koin.compose.viewmodel.koinViewModel

private data class OverviewModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isInverse: Boolean,
    val route: Routes,
)

private val builtInModules = listOf(
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
    /**
     * Dismisses the console. Non-null only when the host has no way out of its own — notably an
     * iOS sheet, where there is no system back button.
     */
    onClose: (() -> Unit)? = null,
) {
    val viewModel = koinViewModel<OverviewViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    val pluginCount = PluginRegistry.getAllPlugins().size
    val (internalPlugins, customPlugins) = remember(pluginCount) {
        PluginRegistry.getDashboardPlugins()
            .map { plugin ->
                OverviewModule(
                    title = plugin.title,
                    subtitle = plugin.description ?: "",
                    // Never inverted. Emphasis is reserved for the first built-in module;
                    // keying it off `priority` scattered white cards through the grid.
                    isInverse = false,
                    route = Routes.Extension(extensionId = plugin.id),
                    icon = plugin.icon ?: Icons.Layers,
                )
            }
            .partition { it.route is Routes.Extension }
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
                    Spacer(Modifier.weight(1f))
                    if (onClose != null) {
                        AlohomoraIconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.X,
                                contentDescription = "Close Alohomora",
                            )
                        }
                    }
                }
                Alohomora.config?.let {
                    Row(
                        modifier = Modifier
                            .padding(top = MaterialTheme.dimens.margin.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = it.projectName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            overflow = TextOverflow.Ellipsis,
                        )
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
                .fillMaxSize(),
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.xl),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            contentPadding = PaddingValues(MaterialTheme.dimens.margin.xl),
            state = lazyGridState,
        ) {
            item(span = { GridItemSpan(4) }) {
                Column {
                    Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
                    Text(
                        "SERVER STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            item(span = { GridItemSpan(4) }) {
                Column {
                    DevToolsStatusCard(
                        state = state,
                        onToggle = { viewModel.onEvent(OverviewEvent.ToggleServer(it)) },
                        onPortChange = { viewModel.onEvent(OverviewEvent.PortChanged(it)) },
                        onRememberChange = {
                            viewModel.onEvent(OverviewEvent.RememberDeviceChanged(it))
                        },
                    )
                }
            }
            if (state.attentionItems.isNotEmpty()) {
                item(span = { GridItemSpan(4) }) {
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.lg))
                        NeedsAttentionPager(
                            items = state.attentionItems,
                            onErrorClick = { error ->
                                onNavigate(Routes.ErrorDetails(error.id))
                            },
                            onTrafficClick = { traffic ->
                                onNavigate(Routes.TrafficDetails(traffic.id))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            item(span = { GridItemSpan(4) }) {
                Column {
                    Spacer(Modifier.height(MaterialTheme.dimens.margin.lg))
                    Text(
                        "SYSTEM MODULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            items(
                items = defaultModules,
                key = { it.route::class.simpleName.orEmpty() },
                span = {
                    if (it.isInverse) GridItemSpan(2)
                    else GridItemSpan(1)
                },
            ) { modules ->
                ModuleCard(modules, onNavigate = onNavigate)
            }
            customPlugins.ifEmpty { null }?.let { plugins ->
                item(span = { GridItemSpan(4) }) {
                    Column {
                        Spacer(Modifier.height(MaterialTheme.dimens.margin.lg))
                        Text(
                            "CUSTOM MODULES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.lg),
                        )
                    }
                }
                items(plugins, key = { it.route::class.simpleName.orEmpty() }) { module ->
                    ModuleCard(module, onNavigate = onNavigate)
                }
            }
        }
    }
}

@Composable
private fun DevToolsStatusCard(
    state: OverviewState,
    onToggle: (Boolean) -> Unit,
    onPortChange: (String) -> Unit,
    onRememberChange: (Boolean) -> Unit,
) {
    Card(
        onClick = {
            onToggle.invoke(!state.serverEnabled)
        },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.dimens.margin.md),
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.md),
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.sm),
                ) {
                    val dotState = when (state.deviceConnectionStatus) {
                        DevConnectionStatus.Connected -> ConnectionDotState.Connected
                        DevConnectionStatus.AwaitingAuth -> ConnectionDotState.Reconnecting
                        DevConnectionStatus.Disconnected,
                        DevConnectionStatus.Off,
                            -> ConnectionDotState.Disconnected
                    }
                    ConnectionStatusDot(state = dotState)
                    Text(
                        when (state.deviceConnectionStatus) {
                            DevConnectionStatus.Connected -> "Connected"
                            DevConnectionStatus.AwaitingAuth -> "Awaiting code"
                            DevConnectionStatus.Disconnected -> "Waiting"
                            DevConnectionStatus.Off -> "Server off"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                Icon(
                    imageVector = if (state.serverEnabled)
                        Icons.ToggleRight
                    else Icons.ToggleLeft,
                    contentDescription = null,
                )
            }
            Spacer(Modifier.height(MaterialTheme.dimens.margin.sm))
            AlohomoraTextField(
                label = "PORT",
                value = state.serverPort,
                onValueChange = onPortChange,
                singleLine = true,
                enabled = !state.serverEnabled,
                modifier = Modifier,
            )
        }
    }

    /*Box(
        modifier = Modifier.fillMaxWidth()
            .border(
                width = MaterialTheme.dimens.stroke.small,
                color = MaterialTheme.colorScheme.onBackground,
                shape = RectangleShape,
            )
            .background(MaterialTheme.colorScheme.background)
            .padding(MaterialTheme.dimens.margin.xl),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Server Status",
                    style = MaterialTheme.typography.headlineSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(
                    checked = state.serverEnabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))

            if (state.pendingOtp != null) {
                AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.xxl))
                Text(
                    "OTP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                Text(
                    state.pendingOtp,
                    style = MaterialTheme.typography.displayMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                Text(
                    "Enter this code on the desktop client",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.md))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRememberChange(!state.rememberDevice) },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.rememberDevice,
                        onCheckedChange = onRememberChange,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                    Text(
                        "Remember this computer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            if (!state.serverError.isNullOrBlank()) {
                AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = MaterialTheme.dimens.margin.lg))
                Text(
                    text = state.serverError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }*/
}

@Composable
private fun ModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
) {

    val containerColor = if (overviewModule.isInverse)
        MaterialTheme.colorScheme.inverseSurface
    else
        MaterialTheme.colorScheme.surfaceContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onNavigate.invoke(overviewModule.route)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            MaterialTheme.dimens.margin.sm,
            Alignment.CenterVertically,
        ),
    ) {
        Icon(
            modifier = Modifier.background(
                color = containerColor,
                shape = MaterialTheme.shapes.large,
            )
                .fillMaxWidth()
                .padding(MaterialTheme.dimens.margin.lg),
            imageVector = overviewModule.icon,
            contentDescription = overviewModule.title,
            tint = contentColorFor(containerColor),
        )
        Text(
            text = overviewModule.title,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** Height of every module tile. See [ModuleCard] for why it is fixed rather than intrinsic. */
private val CARD_HEIGHT = 128.dp

/**
 * Scattered squares in the accent tile's top corner.
 *
 * Purely decorative and deliberately faint — it marks the primary tile without competing with the
 * label. Drawn in a Canvas rather than shipped as an asset so it inherits the content colour and
 * works on either theme.
 */
@Composable
private fun AccentCornerPattern(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(76.dp)) {
        val cell = size.width / 7f
        val square = cell * 0.52f
        for (row in 0 until 7) {
            for (col in 0 until 7) {
                // Densest at the top-right, fading out along the diagonal.
                val distance = (row + (6 - col)) / 12f
                val alpha = (0.18f - distance * 0.24f).coerceAtLeast(0f)
                if (alpha <= 0.01f) continue
                drawRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(col * cell, row * cell),
                    size = Size(square, square),
                )
            }
        }
    }
}
