package io.github.yashkasera.alohomora.presentation.navigation

import kotlinx.serialization.Serializable

internal sealed interface Routes {
    @Serializable
    data object Overview : Routes

    @Serializable
    data object Trace : Routes

    @Serializable
    data class TraceDetails(val traceId: String) : Routes

    @Serializable
    data object Telemetry : Routes

    @Serializable
    data object Cache : Routes

    @Serializable
    data object Config : Routes

    @Serializable
    data object Vault : Routes

    @Serializable
    data object Incident : Routes

    @Serializable
    data class IncidentDetails(val incidentId: Long): Routes

    @Serializable
    data object Chronicle : Routes

    /**
     * Route for extension screens.
     * @param extensionId The unique identifier of the extension
     */
    @Serializable
    data class Extension(val extensionId: String) : Routes
}
