package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Incident

/**
 * Repository for incidents (crashes/errors).
 * Extends base [Repository] with incident-specific delete capability.
 */
internal interface IncidentRepository : Repository<Incident, Long> {

    /**
     * Deletes a specific incident.
     *
     * @param incident Incident to delete
     */
    suspend fun delete(incident: Incident)
}
