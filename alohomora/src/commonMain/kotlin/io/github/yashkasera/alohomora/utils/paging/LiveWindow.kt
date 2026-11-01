package io.github.yashkasera.alohomora.utils.paging

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach

/**
 * The newest rows of a live query, as one `LIMIT n` window that grows by [pageSize] on demand.
 *
 * This replaced an OFFSET pager, and the shape is the fix rather than a simplification of it:
 *
 * - **OFFSET pages over a live table overlap.** Each page was its own `LIMIT 20 OFFSET 20k` flow.
 *   A row inserted at the head shifts every later page by one, so the boundary row appears on two
 *   pages and `items(key = { it.id })` throws on the duplicate key. A single query from offset zero
 *   cannot return the same primary key twice.
 * - **"End reached" is not a permanent fact.** The pager latched it the first time a page came back
 *   short, so a console opened with five captured requests never loaded a second page however many
 *   arrived afterwards. Here [loadMore] asks the *latest* emission whether the window is full, and a
 *   live query re-emits as rows land, so the answer changes with the table.
 *
 * Re-reading `n` rows to grow by one page costs nothing that matters at console scale: the queries
 * already select the list projection, and the alternative held one open flow per page.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class LiveWindow<Filter, Item>(
    initialFilter: Filter,
    private val pageSize: Int,
    private val source: (filter: Filter, limit: Int) -> Flow<List<Item>>,
) {
    private data class Request<F>(val filter: F, val limit: Int)

    /** Which request the latest emission answered, and whether it filled the window it asked for. */
    private data class Answer<F>(val request: Request<F>, val isFull: Boolean)

    private val request = MutableStateFlow(Request(initialFilter, pageSize))
    private val answer = MutableStateFlow<Answer<Filter>?>(null)

    val items: Flow<List<Item>> = request.flatMapLatest { request ->
        source(request.filter, request.limit).onEach { rows ->
            answer.value = Answer(request, isFull = rows.size >= request.limit)
        }
    }

    /**
     * Grows the window by one page, but only once the current window has been delivered *and* came
     * back full. The first guard makes repeated calls while a query is in flight grow it once rather
     * than once per call; the second stops it growing past the end of the table.
     */
    fun loadMore() {
        val current = request.value
        val latest = answer.value ?: return
        if (latest.request != current || !latest.isFull) return
        request.value = current.copy(limit = current.limit + pageSize)
    }

    /** Re-runs the query for [filter] from a single page. A no-op when the filter is unchanged. */
    fun setFilter(filter: Filter) {
        if (request.value.filter == filter) return
        request.value = Request(filter, pageSize)
    }
}
