package io.github.yashkasera.alohomora.utils.paging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

/**
 * The window behind the Traffic, Events and Errors lists.
 *
 * The table is a [MutableStateFlow] standing in for a Room query: newest-first, filtered, and
 * re-emitting on every write the way Room's invalidation does. Ids are what the lists key on, so
 * every assertion is about ids — a duplicate one is the crash this class exists to prevent.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LiveWindowTest {

    /** Rows are ids; larger is newer. */
    private val table = MutableStateFlow((1..50).toList())

    /** Lets a test hold a query "in flight": nothing is delivered while this is false. */
    private val delivering = MutableStateFlow(true)

    private var queriesRun = 0

    private val window = LiveWindow<String, Int>(initialFilter = "", pageSize = PAGE) { filter, limit ->
        queriesRun++
        combine(table, delivering) { rows, open ->
            if (!open) return@combine null
            rows.sortedDescending()
                .filter { filter.isEmpty() || it % 2 == 0 }
                .take(limit)
        }.filterNotNull()
    }

    private fun TestScope.collect(): StateFlow<List<Int>> =
        window.items.stateIn(
            backgroundScope + UnconfinedTestDispatcher(testScheduler),
            SharingStarted.Eagerly,
            emptyList(),
        ).also { settle() }

    private fun TestScope.settle() = testScheduler.advanceUntilIdle()

    @Test
    fun `starts with one page of the newest rows`() = runTest {
        val items = collect()

        assertEquals((50 downTo 41).toList(), items.value)
    }

    @Test
    fun `each loadMore adds one page and stops at the end of the table`() = runTest {
        val items = collect()

        repeat(10) {
            window.loadMore()
            settle()
        }

        assertEquals((50 downTo 1).toList(), items.value)
        // Five pages fill fifty rows and a sixth comes back short. Nothing after that is worth a query.
        assertEquals(6, queriesRun)
    }

    @Test
    fun `a short first window grows once rows arrive`() = runTest {
        table.value = (1..3).toList()
        val items = collect()
        window.loadMore()
        settle()
        assertEquals(listOf(3, 2, 1), items.value)

        table.value = (1..25).toList()
        settle()
        window.loadMore()
        settle()

        assertEquals((25 downTo 6).toList(), items.value)
    }

    @Test
    fun `rows landing at the head never duplicate an id`() = runTest {
        val items = collect()
        window.loadMore()
        settle()
        window.loadMore()
        settle()

        table.value = table.value + (51..55)
        settle()

        assertEquals(items.value.distinct(), items.value)
        assertEquals((55 downTo 26).toList(), items.value)
    }

    @Test
    fun `loadMore while a page is in flight grows the window once`() = runTest {
        val items = collect()
        delivering.value = false

        window.loadMore()
        settle()
        window.loadMore()
        window.loadMore()
        delivering.value = true
        settle()

        assertEquals((50 downTo 31).toList(), items.value)
    }

    @Test
    fun `changing the filter starts again from one page`() = runTest {
        val items = collect()
        window.loadMore()
        settle()
        window.loadMore()
        settle()

        window.setFilter("even")
        settle()

        assertEquals((50 downTo 32 step 2).toList(), items.value)
    }

    private companion object {
        const val PAGE = 10
    }
}
