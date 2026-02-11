package io.github.yashkasera.alohomora.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Routes {
    @Serializable
    data object Dashboard : Routes

    @Serializable
    data object ApiLogs : Routes

    @Serializable
    data class ApiLogDetails(val callId: String) : Routes

    @Serializable
    data object Events : Routes

    @Serializable
    data object Preferences : Routes

    @Serializable
    data object Configuration : Routes

    @Serializable
    data object DatabaseInspector : Routes

    @Serializable
    data object Crashes : Routes

    @Serializable
    data class CrashDetails(val crashId: Long): Routes

    @Serializable
    data object CommitHistory : Routes

    /**
     * Route for custom plugin screens.
     * @param pluginId The unique identifier of the plugin
     */
    @Serializable
    data class CustomPlugin(val pluginId: String) : Routes
}
