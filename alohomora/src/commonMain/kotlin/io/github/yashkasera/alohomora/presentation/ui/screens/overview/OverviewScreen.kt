package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import io.github.yashkasera.alohomora.ui.components.ScrollToTopButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.components.CanvasBackground
import io.github.yashkasera.alohomora.ui.components.ConnectionDotState
import io.github.yashkasera.alohomora.ui.components.ConnectionStatusDot
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraTextField
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraLargeTopAppBar
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopAppBarDefaults
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Settings
import io.github.yashkasera.alohomora.ui.icons.X
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
        title = "Trace",
        subtitle = "LIVE STREAM",
        icon = Icons.Server,
        isInverse = true,
        route = Routes.Trace,
    ),
    OverviewModule(
        title = "Telemetry",
        subtitle = "SYSTEM TRIGGERS",
        icon = Icons.ChartLine,
        isInverse = false,
        route = Routes.Telemetry,
    ),
    OverviewModule(
        title = "Vault",
        subtitle = "Inspector",
        icon = Icons.Database,
        isInverse = false,
        route = Routes.Vault,
    ),
    OverviewModule(
        title = "Incidents",
        subtitle = "critical logs",
        icon = Icons.HardDrive,
        isInverse = false,
        route = Routes.Incident,
    ),
    OverviewModule(
        title = "Cache",
        subtitle = "key-value store",
        icon = Icons.HardDrive,
        isInverse = false,
        route = Routes.Cache,
    ),
    OverviewModule(
        title = "Config",
        subtitle = "build settings",
        icon = Icons.Settings,
        isInverse = false,
        route = Routes.Config,
    ),
    OverviewModule(
        title = "Chronicle",
        subtitle = "commit history",
        icon = Icons.GitGraph,
        isInverse = false,
        route = Routes.Chronicle,
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
                    isInverse = plugin.priority > 5,
                    route = Routes.Extension(extensionId = plugin.id),
                    icon = plugin.icon ?: Icons.Server,
                )
            }
            .partition { it.route is Routes.Extension }
    }
    val defaultModules = remember(internalPlugins) {
        builtInModules + internalPlugins
    }

    val lazyGridState = rememberLazyStaggeredGridState()
    val scrollBehavior = AlohomoraTopAppBarDefaults.enterAlwaysScrollBehavior(
        AlohomoraTopAppBarDefaults.rememberTopAppBarState(),
    )
    val collapsed = 28
    val expanded = 40
    val topAppBarTextSize =
        (collapsed + (expanded - collapsed) * (1 - scrollBehavior.state.collapsedFraction)).sp

    Scaffold(
        topBar = {
            AlohomoraLargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Alohomora",
                            style = MaterialTheme.typography.displayMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = topAppBarTextSize,
                        )
                        if (scrollBehavior.state.collapsedFraction < 0.2f) {
                            Text(
                                "INTERNAL DEVELOPER CONSOLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                },
                actions = {
                    // Rendered only when the host supplies a dismiss handler. This slot
                    // previously held a settings icon with an empty onClick — a control that did
                    // nothing, which on iOS was the *only* button in the bar and left the console
                    // impossible to leave (Compose swallows a sheet's swipe-to-dismiss drag).
                    if (onClose != null) {
                        AlohomoraIconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.X,
                                contentDescription = "Close Alohomora",
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) {
        CanvasBackground()
        Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            modifier = Modifier.padding(it),
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimens.margin.lg),
            verticalItemSpacing = MaterialTheme.dimens.margin.lg,
            contentPadding = PaddingValues(MaterialTheme.dimens.margin.xxl),
            state = lazyGridState,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                DevToolsStatusCard(
                    state = state,
                    onToggle = { viewModel.onEvent(OverviewEvent.ToggleServer(it)) },
                    onPortChange = { viewModel.onEvent(OverviewEvent.PortChanged(it)) },
                    onRememberChange = {
                        viewModel.onEvent(OverviewEvent.RememberDeviceChanged(it))
                    },
                )
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "SYSTEM MODULES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.lg),
                )
            }
            items(defaultModules, key = { it.route::class.simpleName.orEmpty() }) { modules ->
                ModuleCard(modules, onNavigate = onNavigate)
            }
            customPlugins.ifEmpty { null }?.let { plugins ->
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        "CUSTOM MODULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = MaterialTheme.dimens.margin.lg),
                    )
                }
                items(plugins, key = { it.route::class.simpleName.orEmpty() }) { module ->
                    ModuleCard(module, onNavigate = onNavigate)
                }
            }
        }
            ScrollToTopButton(lazyGridState)
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
    Box(
        modifier = Modifier.fillMaxWidth().border(
            MaterialTheme.dimens.stroke.small, MaterialTheme.colorScheme.onBackground, RectangleShape,
        ).background(MaterialTheme.colorScheme.background).padding(MaterialTheme.dimens.margin.xxl),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Server Status",
                    style = MaterialTheme.typography.displaySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Switch(
                    checked = state.serverEnabled,
                    onCheckedChange = onToggle,
                )
            }

            Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.xxl))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "PORT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    AlohomoraTextField(
                        value = state.serverPort,
                        onValueChange = onPortChange,
                        singleLine = true,
                        enabled = !state.serverEnabled,
                        modifier = Modifier.width(120.dp),
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "DEVICE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val dotState = when (state.deviceConnectionStatus) {
                            DevConnectionStatus.Connected -> ConnectionDotState.Connected
                            DevConnectionStatus.AwaitingAuth,
                            DevConnectionStatus.Disconnected -> ConnectionDotState.Reconnecting
                            DevConnectionStatus.Off -> ConnectionDotState.Disconnected
                        }
                        ConnectionStatusDot(state = dotState)
                        Spacer(modifier = Modifier.width(MaterialTheme.dimens.margin.sm))
                        Text(
                            state.deviceConnectionStatus.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }

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

                // Same consent control as the pop-up sheet. Without it, a pairing completed from
                // this screen could never be remembered, which would look like the checkbox
                // simply not working depending on where the user happened to be.
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
                    text = state.serverError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ModuleCard(
    overviewModule: OverviewModule,
    onNavigate: (Routes) -> Unit,
) {
    val backgroundColor =
        if (overviewModule.isInverse) MaterialTheme.colorScheme.inverseSurface
        else MaterialTheme.colorScheme.surface
    val contentColor =
        if (overviewModule.isInverse) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.onSurface
    val borderColor =
        if (overviewModule.isInverse) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .height(IntrinsicSize.Max)
            .border(
                width = MaterialTheme.dimens.stroke.small,
                color = borderColor,
                shape = RectangleShape,
            )
            .background(backgroundColor)
            .clickable {
                onNavigate(overviewModule.route)
            }
            .padding(MaterialTheme.dimens.margin.xl),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Top Section (Icon + Arrow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("→", color = contentColor)
            }

            // Bottom Section (Text)
            Column {
                Text(
                    overviewModule.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontStyle = FontStyle.Italic,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.height(MaterialTheme.dimens.margin.sm))
                Text(
                    overviewModule.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(0.7f),
                )
            }
        }
    }
}
