package io.github.yashkasera.alohomora.presentation.navigation

import kotlinx.serialization.Serializable

internal sealed interface Routes {
    @Serializable
    data object Overview : Routes

    @Serializable
    data object Traffic : Routes

    @Serializable
    data class TrafficDetails(val trafficId: String) : Routes

    @Serializable
    data object Traces : Routes

    @Serializable
    data class TraceDetails(val traceId: String) : Routes

    @Serializable
    data object Events : Routes

    @Serializable
    data object Cache : Routes

    @Serializable
    data object FeatureFlags : Routes

    @Serializable
    data object Config : Routes

    @Serializable
    data object Database : Routes

    @Serializable
    data object Error : Routes

    @Serializable
    data class ErrorDetails(val errorId: Long) : Routes

    @Serializable
    data class Replay(val trafficId: String) : Routes

    @Serializable
    data object GitHistory : Routes

    /**
     * Route for extension screens.
     * @param extensionId The unique identifier of the extension
     */
    @Serializable
    data class Extension(val extensionId: String) : Routes
}
