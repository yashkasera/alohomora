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
import io.github.yashkasera.alohomora.presentation.ui.screens.apilog.detail.ApiLogDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.apilog.list.ApiLogsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.commithistory.CommitHistoryScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.config.ConfigurationScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.crashes.details.CrashDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.crashes.list.CrashListScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.dashboard.DashboardScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.database.DatabaseInspectorScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.events.EventsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.preferences.PreferencesScreen

// Need to detect platform - minimal expect/actual or just resizing for now
// Assuming Desktop has wide screen, Mobile has narrow.
// For true separation we should use expect/actual but WindowSizeClass is better for responsive UI.

@Composable
internal fun AlohomoraNavHost(
    startDestination: Routes = Routes.Dashboard,
) {
    val navController = rememberNavController()

    LaunchedEffect(startDestination) {
        if (startDestination != Routes.Dashboard) {
            navController.navigate(startDestination) {
                launchSingleTop = true
            }
        }
    }

    // Determine layout based on platform/screen size.
    // For this task, we'll try to support both.
    // A simple responsive split:

    // We will use a Row layout that shows NavigationRail on the left if it's "Desktop" (simulated by width)
    // On Mobile, we rely on the Dashboard screen to have buttons, OR we add a BottomBar.
    // The user specifically asked for "Sidebar navigation for desktop".

    // Let's create a responsive shell.
    ResponsiveNavigationShell(navController) {
        NavHost(navController = navController, startDestination = Routes.Dashboard) {
            composable<Routes.Dashboard> {
                DashboardScreen(onNavigate = navController::navigate)
            }

            composable<Routes.ApiLogs> {
                ApiLogsScreen(
                    onLogClick = { id -> navController.navigate(Routes.ApiLogDetails(id)) },
                )
            }

            composable<Routes.Events> {
                EventsScreen(onBackClick = navController::navigateUp)
            }

            composable<Routes.ApiLogDetails> { backStackEntry ->
                val route: Routes.ApiLogDetails = backStackEntry.toRoute()
                ApiLogDetailsScreen(callId = route.callId)
            }

            composable<Routes.Preferences> { backStackEntry ->
                PreferencesScreen(onBackClick = navController::navigateUp)
            }

            composable<Routes.Configuration> { backStackEntry ->
                ConfigurationScreen(
                    onBackClick = navController::navigateUp,
                    onSaveConfig = { url ->
                        // TODO: Handle URL save logic
                        println("Saving BE URL: $url")
                    },
                )
            }

            composable<Routes.DatabaseInspector> {
                DatabaseInspectorScreen(
                    onBackClick = navController::navigateUp,
                )
            }

            composable<Routes.Crashes> {
                CrashListScreen(
                    onBackClick = navController::navigateUp,
                    onNavigateToCrash = {
                        navController.navigate(Routes.CrashDetails(it))
                    },
                )
            }
            composable<Routes.CrashDetails> { backStackEntry ->
                val route: Routes.CrashDetails = backStackEntry.toRoute()
                CrashDetailsScreen(
                    crashId = route.crashId,
                    onBackClick = navController::navigateUp,
                )
            }

            composable<Routes.CommitHistory> {
                CommitHistoryScreen(
                    onBackClick = navController::navigateUp,
                )
            }

            // Dynamic route for custom plugin screens
            composable<Routes.CustomPlugin> { backStackEntry ->
                val route: Routes.CustomPlugin = backStackEntry.toRoute()
                val plugin = PluginRegistry.getPlugin(route.pluginId)

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
                    onClick = { navController.navigate(Routes.Dashboard) },
                    icon = {
//                        Icon(Icons.Default.Home, contentDescription = "Dashboard")
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate(Routes.ApiLogs) },
                    icon = {
//                        Icon(Icons.Default.List, contentDescription = "Logs")
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = CanvasLightGray,
                        indicatorColor = CanvasBlack,
                        unselectedIconColor = CanvasBlack.copy(alpha = 0.5f),
                    ),
                )
                NavigationRailItem(
                    selected = false,
                    onClick = { navController.navigate(Routes.Events) },
                    icon = {
//                        Icon(Icons.Default.Notifications, contentDescription = "Events")
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
