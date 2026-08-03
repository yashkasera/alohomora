package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.Error
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captured errors, **newest first** — matching [EventStore] and [TrafficStore].
 *
 * Ordered and deduplicated by `id` rather than `time`, unlike [EventStore]: the device streams these
 * `ORDER BY id DESC` because ids are monotonic, and a crash plus the `App.Exception` event it
 * mirrors are recorded in the same millisecond. Sorting those by time is a coin flip.
 */
class ErrorStore {
    private val _errors = MutableStateFlow<List<Error>>(emptyList())
    val errors: StateFlow<List<Error>> = _errors.asStateFlow()

    /**
     * Inserts [error] at the top, ignoring a redelivery of one already held.
     *
     * An error is immutable once recorded, so there is nothing to upsert — but the device reseeds
     * its stream adapter on every snapshot, so the same row can legitimately arrive twice.
     */
    fun append(error: Error) {
        val current = _errors.value
        if (current.any { it.id == error.id }) return
        _errors.value = (listOf(error) + current).take(MAX_ENTRIES)
    }

    /** Replaces everything, normalising to newest-first rather than trusting the wire order. */
    fun replace(errors: List<Error>) {
        _errors.value = errors
            .sortedByDescending { it.id }
            .take(MAX_ENTRIES)
    }

    /** Dims an error opened in this window; see TrafficStore.markViewed for why this stays local. */
    fun markViewed(id: Long) {
        val current = _errors.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].isViewed) return
        _errors.value = current.toMutableList().also { list ->
            list[index] = list[index].copy(isViewed = true)
        }
    }

    fun clear() {
        _errors.value = emptyList()
    }

    private companion object {
        /**
         * An order of magnitude below the event cap: every row carries a full stack trace, and the
         * device only ever sends `ERROR_SNAPSHOT_LIMIT` (100) of them per snapshot anyway.
         */
        const val MAX_ENTRIES = 200
    }
}
