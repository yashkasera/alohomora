package io.github.yashkasera.alohomora.domain.repository

import io.github.yashkasera.alohomora.common.Error

/**
 * Repository for incidents (crashes/errors).
 * Extends base [Repository] with incident-specific delete capability.
 */
internal interface ErrorRepository : Repository<Error, Long> {

    /**
     * Deletes a specific incident.
     *
     * @param error Error to delete
     */
    suspend fun delete(error: Error)
}
