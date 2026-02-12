package io.github.yashkasera.alohomora.presentation.ui.screens.dashboard

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
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Icons
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Server
import io.github.yashkasera.alohomora.presentation.ui.components.icons.Settings
import io.github.yashkasera.alohomora.presentation.ui.components.icons.chartLine
import io.github.yashkasera.alohomora.presentation.ui.components.icons.database
import io.github.yashkasera.alohomora.presentation.ui.components.icons.gitGraph
import io.github.yashkasera.alohomora.presentation.ui.components.icons.hardDrive
import io.github.yashkasera.alohomora.ui.components.AlohomoraHorizontalDivider
import io.github.yashkasera.alohomora.ui.components.AlohomoraIconButton
import io.github.yashkasera.alohomora.ui.components.AlohomoraLargeTopAppBar
import io.github.yashkasera.alohomora.ui.components.AlohomoraTopAppBarDefaults
import org.koin.compose.viewmodel.koinViewModel

private data class DashboardModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val isInverse: Boolean,
    val route: Routes,
)

private fun getModules() = listOf(
    DashboardModule(
        title = "API Logs",
        subtitle = "LIVE STREAM",
        icon = Icons.Server,
        isInverse = true,
        route = Routes.ApiLogs,
    ),
    DashboardModule(
        title = "Events",
        subtitle = "SYSTEM TRIGGERS",
        icon = Icons.chartLine,
        isInverse = false,
        route = Routes.Events,
    ),
    DashboardModule(
        title = "Database",
        subtitle = "Inspector",
        icon = Icons.database,
        isInverse = false,
        route = Routes.DatabaseInspector,
    ),
    DashboardModule(
        title = "Crashes",
        subtitle = "critical logs",
        icon = Icons.hardDrive,
        isInverse = false,
        route = Routes.Crashes,
    ),
    DashboardModule(
        title = "Preferences",
        subtitle = "memory store",
        icon = Icons.hardDrive,
        isInverse = false,
        route = Routes.Preferences,
    ),
    DashboardModule(
        title = "Config",
        subtitle = "build settings",
        icon = Icons.Settings,
        isInverse = false,
        route = Routes.Configuration,
    ),
    DashboardModule(
        title = "Git ",
        subtitle = "commit history",
        icon = Icons.gitGraph,
        isInverse = false,
        route = Routes.CommitHistory,
    ),
)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    onNavigate: (Routes) -> Unit,
) {
    val viewModel = koinViewModel<DashboardViewModel>()
    val state by viewModel.state.collectAsState()

    val (internalPlugins, customPlugins) = remember {
        PluginRegistry.getDashboardPlugins()
            .map { plugin ->
                DashboardModule(
                    title = plugin.title,
                    subtitle = plugin.description ?: "",
                    isInverse = plugin.priority > 5,
                    route = Routes.CustomPlugin(pluginId = plugin.id),
                    icon = plugin.icon,
                )
            }
            .partition { it.route is Routes.CustomPlugin }
    }
    val defaultModules = remember {
        getModules() + internalPlugins
    }

    val lazyGridState = rememberLazyStaggeredGridState()
    val scrollBehavior = AlohomoraTopAppBarDefaults.enterAlwaysScrollBehavior(
        AlohomoraTopAppBarDefaults.rememberTopAppBarState()
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
//                     Icon(Icons.Outlined.Settings, "Settings", tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(16.dp))
//                     Icon(Icons.Outlined.Info, "Help", tint = MaterialTheme.colorScheme.tertiary)
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
    dashboardModule: DashboardModule,
    onNavigate: (Routes) -> Unit,
) {
    val backgroundColor =
        if (dashboardModule.isInverse) MaterialTheme.colorScheme.inverseSurface
        else MaterialTheme.colorScheme.surface
    val contentColor =
        if (dashboardModule.isInverse) MaterialTheme.colorScheme.inverseOnSurface
        else MaterialTheme.colorScheme.onSurface
    val borderColor =
        if (dashboardModule.isInverse) MaterialTheme.colorScheme.inverseOnSurface
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
                onNavigate(dashboardModule.route)
            }
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            // Top Section (Icon + Arrow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                /* Icon(
                     imageVector = icon,
                     contentDescription = null,
                     tint = contentColor
                 )*/

                // Arrow, maybe only if not gray? Mock has arrows on top 3.
                Text("→", color = contentColor)
            }

            // Bottom Section (Text)
            Column {
                Text(
                    dashboardModule.title,
                    style = MaterialTheme.typography.headlineLarge,
                    fontStyle = FontStyle.Italic,
                    color = contentColor,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    dashboardModule.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(0.7f),
                )
            }
        }
    }
}
