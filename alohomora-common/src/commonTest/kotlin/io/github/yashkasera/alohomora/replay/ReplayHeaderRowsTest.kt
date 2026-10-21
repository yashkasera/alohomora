package io.github.yashkasera.alohomora.replay

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The structured header editor's rows -> wire-map collapse is the one non-obvious piece of the
 * replay sheet: an excluded row must vanish so the app's client regenerates the header, a blank
 * name must not become an empty header, and repeated names must group like [ReplayHeaderText] does.
 *
 * Lives in `alohomora-common` because both consoles edit headers through these rows; the desktop
 * sheet and the mobile replay screen must collapse them identically.
 */
class ReplayHeaderRowsTest {

    @Test
    fun keepsIncludedRowsAndTrims() {
        val rows = listOf(
            ReplayHeaderRow(0, "  Content-Type ", " application/json ", included = true),
            ReplayHeaderRow(1, "Accept", "*/*", included = true),
        )
        assertEquals(
            mapOf("Content-Type" to listOf("application/json"), "Accept" to listOf("*/*")),
            buildReplayHeaderMap(rows),
        )
    }

    @Test
    fun dropsExcludedRows() {
        val rows = listOf(
            ReplayHeaderRow(0, "x-signature", "abc", included = false),
            ReplayHeaderRow(1, "Accept", "*/*", included = true),
        )
        assertEquals(mapOf("Accept" to listOf("*/*")), buildReplayHeaderMap(rows))
    }

    @Test
    fun dropsBlankNames() {
        val rows = listOf(
            ReplayHeaderRow(0, "   ", "orphan", included = true),
            ReplayHeaderRow(1, "Accept", "*/*", included = true),
        )
        assertEquals(mapOf("Accept" to listOf("*/*")), buildReplayHeaderMap(rows))
    }

    @Test
    fun groupsRepeatedNamesInOrder() {
        val rows = listOf(
            ReplayHeaderRow(0, "Set-Cookie", "a=1", included = true),
            ReplayHeaderRow(1, "Set-Cookie", "b=2", included = true),
        )
        assertEquals(mapOf("Set-Cookie" to listOf("a=1", "b=2")), buildReplayHeaderMap(rows))
    }

    @Test
    fun seedsOneRowPerValueFromWireMap() {
        var next = 0L
        val rows = mapOf("Set-Cookie" to listOf("a=1", "b=2"), "Accept" to listOf("*/*"))
            .toReplayHeaderRows { next++ }

        assertEquals(3, rows.size)
        assertEquals(listOf("Set-Cookie", "Set-Cookie", "Accept"), rows.map { it.name })
        assertEquals(listOf("a=1", "b=2", "*/*"), rows.map { it.value })
        assertEquals(true, rows.all { it.included })
        assertEquals(listOf(0L, 1L, 2L), rows.map { it.key })
    }
}
