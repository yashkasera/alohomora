package io.github.yashkasera.alohomora.desktop.data.local

import io.github.yashkasera.alohomora.common.TraceEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Captured API traces, **newest first**.
 *
 * Ordering is owned here rather than at each render site. The device's initial snapshot arrives
 * `ORDER BY time DESC` (newest first) but [append] used to add to the end, so streamed traces
 * landed at the bottom while the snapshot above them read top-down — and the dashboard applied its
 * own `asReversed()`, producing two different orders for the same data inside one app.
 */
class ApiLogStore {
    private val _logs = MutableStateFlow<List<TraceEntry>>(emptyList())
    val logs: StateFlow<List<TraceEntry>> = _logs.asStateFlow()

    /**
     * Inserts [log] at the top, or updates it in place when already present.
     *
     * The upsert matters: the device re-sends a trace whenever its contents change, so a request
     * captured in flight and then completed arrives twice with the same id. A plain append showed
     * it twice — once pending, once complete — which becomes glaring once ordering is fixed and
     * the two copies sit adjacent at the top.
     *
     * An updated trace keeps its position rather than jumping to the top, since `time` is the
     * request *start* time and that is what the list is ordered by.
     */
    fun append(log: TraceEntry) {
        val current = _logs.value
        val existing = current.indexOfFirst { it.id == log.id }
        _logs.value = if (existing >= 0) {
            current.toMutableList().also { it[existing] = log }
        } else {
            // take, not takeLast: the newest entry is at the head now, so trimming the tail is
            // what drops the oldest.
            (listOf(log) + current).take(MAX_ENTRIES)
        }
    }

    /** Replaces everything, normalising to newest-first rather than trusting the wire order. */
    fun replace(logs: List<TraceEntry>) {
        _logs.value = logs
            .sortedByDescending { it.time ?: Long.MIN_VALUE }
            .take(MAX_ENTRIES)
    }

    /**
     * Dims a trace the user has opened in this window.
     *
     * Marked locally rather than round-tripping to the device: "viewed" means viewed *here*, and
     * a desktop-initiated write would also fight the device's own snapshot, which carries its own
     * isViewed and would overwrite ours on the next refresh.
     */
    fun markViewed(id: String) {
        val current = _logs.value
        val index = current.indexOfFirst { it.id == id }
        if (index < 0 || current[index].isViewed) return
        _logs.value = current.toMutableList().also { list ->
            list[index] = list[index].copy(isViewed = true)
        }
    }

    fun clear() {
        _logs.value = emptyList()
    }

    private companion object {
        const val MAX_ENTRIES = 2000
    }
}
