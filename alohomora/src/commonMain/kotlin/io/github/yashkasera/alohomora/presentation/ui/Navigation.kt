package io.github.yashkasera.alohomora.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.yashkasera.alohomora.plugin.PluginRegistry
import io.github.yashkasera.alohomora.presentation.navigation.Routes
import io.github.yashkasera.alohomora.presentation.ui.screens.cache.CacheScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.config.ConfigScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.database.DatabaseScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.error.detail.ErrorDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.error.list.ErrorScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.events.EventsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.githistory.GitHistoryScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.overview.OverviewScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.traffic.detail.TrafficDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.traffic.list.TrafficScreen

// Need to detect platform - minimal expect/actual or just resizing for now
// Assuming Desktop has wide screen, Mobile has narrow.
// For true separation we should use expect/actual but WindowSizeClass is better for responsive UI.

@Composable
internal fun AlohomoraNavHost(
    startDestination: Routes = Routes.Overview,
    onClose: (() -> Unit)? = null,
) {
    val navController = rememberNavController()

    LaunchedEffect(startDestination) {
        if (startDestination != Routes.Overview) {
            navController.navigate(startDestination) {
                launchSingleTop = true
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Overview) {
        composable<Routes.Overview> {
            OverviewScreen(onNavigate = navController::navigate, onClose = onClose)
        }

        composable<Routes.Traffic> {
            TrafficScreen(
                onTraceClick = { id -> navController.navigate(Routes.TrafficDetails(id)) },
                onBackClick = navController::navigateUp
            )
        }

        composable<Routes.Events> {
            EventsScreen(onBackClick = navController::navigateUp)
        }

        composable<Routes.TrafficDetails> { backStackEntry ->
            val route: Routes.TrafficDetails = backStackEntry.toRoute()
            TrafficDetailsScreen(
                traceId = route.trafficId,
                onBackClick = navController::navigateUp,
                onOpenTrace = { id -> navController.navigate(Routes.TrafficDetails(id)) },
            )
        }

        composable<Routes.Cache> {
            CacheScreen(onBackClick = navController::navigateUp)
        }

        composable<Routes.Config> {
            ConfigScreen(
                onBackClick = navController::navigateUp,
                onSaveConfig = { url ->
                    // TODO: Handle URL save logic
                    println("Saving BE URL: $url")
                },
            )
        }

        composable<Routes.Database> {
            DatabaseScreen(onBackClick = navController::navigateUp)
        }

        composable<Routes.Error> {
            ErrorScreen(
                onBackClick = navController::navigateUp,
                onNavigateToError = {
                    navController.navigate(Routes.ErrorDetails(it))
                },
            )
        }
        composable<Routes.ErrorDetails> { backStackEntry ->
            val route: Routes.ErrorDetails = backStackEntry.toRoute()
            ErrorDetailsScreen(
                errorId = route.errorId,
                onBackClick = navController::navigateUp,
            )
        }

        composable<Routes.GitHistory> {
            GitHistoryScreen(
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
