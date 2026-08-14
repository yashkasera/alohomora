package io.github.yashkasera.alohomora.desktop.presentation.model

import io.github.yashkasera.alohomora.common.TrafficEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The Traffic panel's search and method filter. */
class TrafficFilterTest {

    private fun entry(
        id: String,
        method: String? = "GET",
        url: String = "https://api.example.com/v1/posts",
        path: String? = "/v1/posts",
        status: Int? = 200,
    ) = TrafficEntry(id = id, method = method, url = url, path = path, status = status)

    private fun index(vararg entries: TrafficEntry) =
        entries.map { IndexedTraffic(it, it.searchHaystack()) }

    private fun shown(filters: TrafficFilterState, vararg entries: TrafficEntry) =
        index(*entries).filterTraffic(filters).map { it.id }

    @Test
    fun `no filters shows everything`() {
        val ids = shown(TrafficFilterState(), entry("a"), entry("b", method = "POST"))

        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun `an empty method set shows everything rather than nothing`() {
        // The include set's default. Read the other way round an empty selection would hide the whole list,
        // which is the failure mode the KDoc on TrafficFilterState calls out.
        val ids =
            shown(TrafficFilterState(methods = emptySet()), entry("a"), entry("b", method = "POST"))

        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun `selecting a method hides the others`() {
        val ids = shown(
            TrafficFilterState(methods = setOf("POST")),
            entry("a", method = "GET"),
            entry("b", method = "POST"),
        )

        assertEquals(listOf("b"), ids)
    }

    @Test
    fun `two selected methods both show`() {
        val ids = shown(
            TrafficFilterState(methods = setOf("GET", "DELETE")),
            entry("a", method = "GET"),
            entry("b", method = "POST"),
            entry("c", method = "DELETE"),
        )

        assertEquals(listOf("a", "c"), ids)
    }

    @Test
    fun `a lowercase method still matches its chip`() {
        // Both the chip label and the filter go through methodLabel, so a device reporting "get" cannot
        // produce a row no chip can select.
        val ids = shown(TrafficFilterState(methods = setOf("GET")), entry("a", method = "get"))

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `a request with no method is filterable as unknown`() {
        val entry = entry("a", method = null)

        assertEquals("UNKNOWN", entry.methodLabel())
        assertEquals(listOf("a"), shown(TrafficFilterState(methods = setOf("UNKNOWN")), entry))
    }

    @Test
    fun `a query matches part of the url`() {
        val ids = shown(
            TrafficFilterState(query = "v1/posts"),
            entry("a", url = "https://api.example.com/v1/posts"),
            entry("b", url = "https://api.example.com/v1/users"),
        )

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `a query matches the method`() {
        val ids = shown(
            TrafficFilterState(query = "post"),
            entry("a", method = "POST", url = "https://api.example.com/v1/users"),
            entry("b", method = "GET", url = "https://api.example.com/v1/users"),
        )

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `a query matches the status code`() {
        val ids = shown(
            TrafficFilterState(query = "500"),
            entry("a", status = 200),
            entry("b", status = 500),
        )

        assertEquals(listOf("b"), ids)
    }

    @Test
    fun `a query is case-insensitive and trimmed`() {
        val ids = shown(TrafficFilterState(query = "  API.EXAMPLE  "), entry("a"))

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `a query and a method must both match`() {
        val ids = shown(
            TrafficFilterState(query = "users", methods = setOf("POST")),
            entry("a", method = "POST", url = "https://api.example.com/v1/users"),
            entry("b", method = "GET", url = "https://api.example.com/v1/users"),
            entry("c", method = "POST", url = "https://api.example.com/v1/posts"),
        )

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `a body is not searched`() {
        // Bodies are the largest captured fields; scanning them on every keystroke would cost far more
        // than the endpoint hunt this box is for.
        val withBody = entry("a").also { it.responseBody = "{\"secret\":\"needle\"}" }

        assertTrue(shown(TrafficFilterState(query = "needle"), withBody).isEmpty())
    }

    @Test
    fun `an entry with no url falls back to its path for searching`() {
        val ids = shown(
            TrafficFilterState(query = "/v1/posts"),
            entry("a", url = "", path = "/v1/posts"),
        )

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `toggling a method adds then removes it`() {
        val once = TrafficFilterState().withMethodToggled("GET")
        assertEquals(setOf("GET"), once.methods)
        assertTrue(once.withMethodToggled("GET").methods.isEmpty())
    }

    @Test
    fun `hasFilter reflects a query or a method but not an empty state`() {
        assertFalse(TrafficFilterState().hasFilter)
        assertTrue(TrafficFilterState(query = "x").hasFilter)
        assertTrue(TrafficFilterState(methods = setOf("GET")).hasFilter)
        assertFalse(TrafficFilterState(query = "   ").hasFilter)
    }

    @Test
    fun `clearing drops both axes`() {
        val cleared = TrafficFilterState(query = "x", methods = setOf("GET")).cleared()

        assertFalse(cleared.hasFilter)
    }

    @Test
    fun `a subtitle reports the shown count only when filtering hides something`() {
        val unfiltered = TrafficUiState(entries = listOf(entry("a")), totalCount = 1)
        assertEquals("1 request", trafficSubtitle(unfiltered))

        val filtered = TrafficUiState(entries = listOf(entry("a")), totalCount = 12)
        assertEquals("12 requests · 1 shown", trafficSubtitle(filtered))
    }

    @Test
    fun `errors only keeps four and five hundreds`() {
        val ids = shown(
            TrafficFilterState(errorsOnly = true),
            entry("ok", status = 200),
            entry("redirect", status = 301),
            entry("notFound", status = 404),
            entry("serverError", status = 500),
        )

        assertEquals(listOf("notFound", "serverError"), ids)
    }

    @Test
    fun `a response with no status is not an error`() {
        // Null means no response code was seen — in flight, or never captured. Calling that a failure would
        // put requests in the Errors view that may well have succeeded.
        val pending = entry("pending", status = null)

        assertFalse(pending.isError())
        assertTrue(shown(TrafficFilterState(errorsOnly = true), pending).isEmpty())
    }

    @Test
    fun `the error floor is four hundred inclusive`() {
        assertFalse(entry("a", status = 399).isError())
        assertTrue(entry("b", status = 400).isError())
    }

    @Test
    fun `errors only composes with a method filter`() {
        val ids = shown(
            TrafficFilterState(methods = setOf("POST"), errorsOnly = true),
            entry("a", method = "POST", status = 500),
            entry("b", method = "GET", status = 500),
            entry("c", method = "POST", status = 200),
        )

        assertEquals(listOf("a"), ids)
    }

    @Test
    fun `errors only counts as a filter and is cleared with the rest`() {
        val filters = TrafficFilterState(errorsOnly = true)

        assertTrue(filters.hasFilter)
        assertFalse(filters.cleared().hasFilter)
    }
}
