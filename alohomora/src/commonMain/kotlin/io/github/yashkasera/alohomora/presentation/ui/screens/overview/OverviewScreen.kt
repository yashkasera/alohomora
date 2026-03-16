package io.github.yashkasera.alohomora.presentation.ui.screens.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import io.github.yashkasera.alohomora.ui.icons.Icons
import io.github.yashkasera.alohomora.ui.icons.Server
import io.github.yashkasera.alohomora.ui.icons.Settings
import io.github.yashkasera.alohomora.ui.icons.ChartLine
import io.github.yashkasera.alohomora.ui.icons.Database
import io.github.yashkasera.alohomora.ui.icons.GitGraph
import io.github.yashkasera.alohomora.ui.icons.HardDrive
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraLargeTopAppBar
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopAppBarDefaults
import org.koin.compose.viewmodel.koinViewModel

private data class OverviewModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isInverse: Boolean,
    val route: Routes,
)

private fun getModules() = listOf(
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
) {
    val viewModel = koinViewModel<OverviewViewModel>()
    val state by viewModel.state.collectAsState()

    val (internalPlugins, customPlugins) = remember {
        PluginRegistry.getDashboardPlugins()
            .map { plugin ->
                OverviewModule(
                    title = plugin.title,
                    subtitle = plugin.description ?: "",
                    isInverse = plugin.priority > 5,
                    route = Routes.Extension(extensionId = plugin.id),
                    icon = plugin.icon,
                )
            }
            .partition { it.route is Routes.Extension }
    }
    val defaultModules = remember {
        getModules() + internalPlugins
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
                    AlohomoraIconButton(
                        onClick = {

                        },
                    ) {
                        Icon(
                            imageVector = Icons.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "CLIENT VERSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        "v2.4.0 (Build 492)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Row {
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        },
    ) {
        CanvasBackground()
        LazyVerticalStaggeredGrid(
            modifier = Modifier.padding(it),
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalItemSpacing = 16.dp,
            contentPadding = PaddingValues(24.dp),
            state = lazyGridState,
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ActiveTargetCard(isConnected = state.isConnected)
            }
            item(span = StaggeredGridItemSpan.FullLine) {
                Text(
                    "SYSTEM MODULES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
            items(defaultModules) { modules ->
                ModuleCard(modules, onNavigate = onNavigate)
            }
            customPlugins.ifEmpty { null }?.let { plugins ->
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        "CUSTOM MODULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
                items(plugins) { module ->
                    ModuleCard(module, onNavigate = onNavigate)
                }
            }
        }
    }
}

@Composable
fun ActiveTargetCard(isConnected: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().border(
            1.dp, MaterialTheme.colorScheme.onBackground, RectangleShape,
        ).background(MaterialTheme.colorScheme.background).padding(24.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    "ACTIVE TARGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )

                // Status Badge
                Box(
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50),
                    ).padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.error,
                                    shape = CircleShape,
                                ),
                        ) // Status Dot
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isConnected) "CONNECTED" else "DISCONNECTED",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Pixel 7 Pro",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                "ID: 8A:2F:91:00",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp),
            )

            AlohomoraHorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Icon placeholder for wifi/network
                        Text("◎", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "LOCAL HOST",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                    Text(
                        "192.168.1.42",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(
                            top = 8.dp, start = 20.dp,
                        ), // Indent to align with text above
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "LATENCY",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "12ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
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
                width = 1.dp,
                color = borderColor,
                shape = RectangleShape,
            )
            .background(backgroundColor)
            .clickable {
                onNavigate(overviewModule.route)
            }
            .padding(20.dp),
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
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    overviewModule.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(0.7f),
                )
            }
        }
    }
}

