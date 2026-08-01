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
import io.github.yashkasera.alohomora.presentation.ui.screens.chronicle.ChronicleScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.config.ConfigScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.incident.detail.IncidentDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.incident.list.IncidentScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.overview.OverviewScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.telemetry.TelemetryScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.trace.detail.TraceDetailsScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.trace.list.TraceScreen
import io.github.yashkasera.alohomora.presentation.ui.screens.vault.VaultScreen

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

        composable<Routes.Trace> {
            TraceScreen(
                onTraceClick = { id -> navController.navigate(Routes.TraceDetails(id)) },
                onBackClick = navController::navigateUp
            )
        }

        composable<Routes.Telemetry> {
            TelemetryScreen(onBackClick = navController::navigateUp)
        }

        composable<Routes.TraceDetails> { backStackEntry ->
            val route: Routes.TraceDetails = backStackEntry.toRoute()
            TraceDetailsScreen(
                traceId = route.traceId,
                onBackClick = navController::navigateUp,
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

        composable<Routes.Vault> {
            VaultScreen(onBackClick = navController::navigateUp)
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
