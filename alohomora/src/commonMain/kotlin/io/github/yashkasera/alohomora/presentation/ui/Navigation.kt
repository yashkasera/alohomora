package io.github.yashkasera.alohomora.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.yashkasera.alohomora.Platform
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.ui.theme.CanvasBlack
import io.github.yashkasera.alohomora.ui.theme.CanvasLightGray
import io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail.TraceDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.trace.list.TraceScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.telemetry.TelemetryScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.incident.detail.IncidentDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.incident.list.IncidentScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.vault.VaultScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.cache.CacheScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.chronicle.ChronicleScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.config.ConfigScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.overview.OverviewScreen

// Need to detect platform - minimal expect/actual or just resizing for now
// Assuming Desktop has wide screen, Mobile has narrow.
// For true separation we should use expect/actual but WindowSizeClass is better for responsive UI.

@Composable
internal fun AlohomoraNavHost(
    startDestination: Routes = Routes.Overview,
) {
    val navController = rememberNavController()

    LaunchedEffect(startDestination) {
        if (startDestination != Routes.Overview) {
            navController.navigate(startDestination) {
                launchSingleTop = true
            }
        }
    }

    // Determine layout based on platform/screen size.
    // For this task, we'll try to support both.
    // A simple responsive split:

    // We will use a Row layout that shows NavigationRail on the left if it's "Desktop" (simulated by width)
    // On Mobile, we rely on the Overview screen to have buttons, OR we add a BottomBar.
    // The user specifically asked for "Sidebar navigation for desktop".

    // Let's create a responsive shell.
    ResponsiveNavigationShell(navController) {
        NavHost(navController = navController, startDestination = Routes.Overview) {
            composable<Routes.Overview> {
                OverviewScreen(onNavigate = navController::navigate)
            }

            composable<Routes.Trace> {
                TraceScreen(
                    onTraceClick = { id -> navController.navigate(Routes.TraceDetails(id)) },
                )
            }

            composable<Routes.Telemetry> {
                TelemetryScreen(onBackClick = navController::navigateUp)
            }

            composable<Routes.TraceDetails> { backStackEntry ->
                val route: Routes.TraceDetails = backStackEntry.toRoute()
                TraceDetailsScreen(traceId = route.traceId)
            }

            composable<Routes.Cache> { backStackEntry ->
                CacheScreen(onBackClick = navController::navigateUp)
            }

            composable<Routes.Config> { backStackEntry ->
                ConfigScreen(
                    onBackClick = navController::navigateUp,
                    onSaveConfig = { url ->
                        // TODO: Handle URL save logic
                        println("Saving BE URL: $url")
                    },
                )
            }

            composable<Routes.Vault> {
                VaultScreen(
                    onBackClick = navController::navigateUp,
                )
            }

            composable<Routes.Incident> {
                IncidentScreen(
                    onBackClick = navController::navigateUp,
                    onNavigateToIncident = {
                        navController.navigate(Routes.IncidentDetails(it))
                    },
                )
            }
            composable<Routes.IncidentDetails> { backStackEntry ->
                val route: Routes.IncidentDetails = backStackEntry.toRoute()
                IncidentDetailsScreen(
                    incidentId = route.incidentId,
                    onBackClick = navController::navigateUp,
                )
            }

            composable<Routes.Chronicle> {
                ChronicleScreen(
                    onBackClick = navController::navigateUp,
                )
            }

            // Dynamic route for custom plugin screens
            composable<Routes.Extension> { backStackEntry ->
                val route: Routes.Extension = backStackEntry.toRoute()
                val plugin = PluginRegistry.getPlugin(route.extensionId)

                if (plugin != null) {
                    plugin.Content(onBackClick = navController::navigateUp)
                } else {
                    // Fallback if plugin not found - navigate back
                    navController.navigateUp()
                }
            }
        }
    }
}

@Composable
private fun ResponsiveNavigationShell(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
//        .systemBarsPadding()
    ) {
        if (Platform.isDesktop) {
            NavigationRail(
                containerColor = CanvasLightGray.copy(alpha = 0.5f),
                contentColor = CanvasBlack,
                modifier = Modifier.width(80.dp).fillMaxHeight(),
            ) {
                NavigationRailItem(
                    selected = true, // Simplified for now, real app would check route
                    onClick = { navController.navigate(Routes.Overview) },
                    icon = {
//                        Icon(Icons.Default.Home, contentDescription = "Overview")
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate(Routes.Trace) },
                    icon = {
//                        Icon(Icons.Default.List, contentDescription = "Trace")
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = CanvasLightGray,
                        indicatorColor = CanvasBlack,
                        unselectedIconColor = CanvasBlack.copy(alpha = 0.5f),
                    ),
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate(Routes.Telemetry) },
                    icon = {
//                        Icon(Icons.Default.Notifications, contentDescription = "Telemetry")
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = CanvasLightGray,
                        indicatorColor = CanvasBlack,
                        unselectedIconColor = CanvasBlack.copy(alpha = 0.5f),
                    ),
                )
            }
        }

        // Content Area
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}
