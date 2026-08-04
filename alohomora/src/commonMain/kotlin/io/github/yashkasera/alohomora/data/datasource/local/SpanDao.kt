package io.github.yashkasera.alohomora.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.yashkasera.alohomora.common.Span
import kotlinx.coroutines.flow.Flow

/**
 * DAO for [Span] entities.
 *
 * Deliberately has **no trace-aggregate query**. Grouping spans into traces happens in shared Kotlin
 * (`List<Span>.toTraceSummaries()`), because the desktop has no database and must group streamed
 * spans anyway — a `GROUP BY traceId` here would be a second implementation of the same definition,
 * and the untestable one of the two. That divergence is how the two consoles came to disagree on an
 * error row's title before `exceptionTypeName()` moved into `:alohomora-common`.
 */
@Dao
internal interface SpanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: Span): Long

    /**
     * One Room transaction for a whole batch, which is the shape a tracer's exporter hands over.
     *
     * REPLACE rather than IGNORE on the unique `spanId` index: a tracer that re-exports after a
     * failed flush should update the row, not be silently dropped.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<Span>)

    /** Every span of one trace, in start order — the waterfall's input. */
    @Query("SELECT * FROM Span WHERE traceId = :traceId ORDER BY startEpochNanos ASC")
    fun observeTrace(traceId: String): Flow<List<Span>>

    /** Snapshot counterpart to [observeTrace], for answering `REQUEST_TRACE_SPANS`. */
    @Query("SELECT * FROM Span WHERE traceId = :traceId ORDER BY startEpochNanos ASC")
    suspend fun getTrace(traceId: String): List<Span>

    /**
     * Newest-first by rowid, not by timestamp.
     *
     * `id` because nothing time-based here is monotonic: a parent ends after its children, a
     * later-started sibling can end before an earlier one, and two spans can share a timestamp. The
     * same key drives `DevToolsStreamAdapter`, which drops anything not strictly greater than the
     * last key it saw — so an end-ordered query would silently lose spans. Same reasoning as
     * [ErrorDao.getLatest].
     */
    @Query("SELECT * FROM Span ORDER BY id DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<Span>

    /** Reactive counterpart to [getLatest], for streaming to the desktop and the mobile list. */
    @Query("SELECT * FROM Span ORDER BY id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<Span>>

    /** Marks a whole trace viewed: viewing is a trace-level act, so every span in it is flipped. */
    @Query("UPDATE Span SET isViewed = 1 WHERE traceId = :traceId")
    suspend fun markTraceAsViewed(traceId: String)

    @Query("DELETE FROM Span")
    suspend fun clearAll()
}
