package io.github.yashkasera.alohomora.ui.components.jsonviewer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class VisibleTreeStateTest {

    private fun stateFrom(json: String): VisibleTreeState {
        val tree = JsonTreeBuilder.build(Json.parseToJsonElement(json))
        return VisibleTreeState(tree)
    }

    @Test
    fun `collapse then expand resets children to collapsed`() {
        val state = stateFrom("""[{"a":1},{"b":2},{"c":3}]""")

        // Expand child 0
        val child0 = "$[0]"
        state.expand(child0)
        assertTrue(state.isExpanded(child0))

        // Collapse the root array
        state.toggle("$")
        assertFalse(state.isExpanded("$"))
        assertFalse(state.isExpanded(child0), "child must leave expanded set on parent collapse")

        // Re-expand root
        state.toggle("$")
        assertTrue(state.isExpanded("$"))
        assertFalse(state.isExpanded(child0), "child must not be expanded after parent re-expand")
    }

    @Test
    fun `deeply nested expand state is cleared on ancestor collapse`() {
        val state = stateFrom("""{"a":{"b":{"c":1}}}""")

        state.expand("$.a")
        state.expand("$.a.b")
        assertTrue(state.isExpanded("$.a"))
        assertTrue(state.isExpanded("$.a.b"))

        state.toggle("$")

        assertFalse(state.isExpanded("$.a"))
        assertFalse(state.isExpanded("$.a.b"))
    }

    @Test
    fun `collapse and re-expand produces correct row count`() {
        val json = """[{"id":1},{"id":2},{"id":3}]"""
        val state = stateFrom(json)

        // Root is expanded by init: OPEN($), OPEN([0]), OPEN([1]), OPEN([2]), CLOSE($)
        val countAfterInit = state.rows.size

        state.toggle("$") // collapse
        // Only OPEN($) remains
        assertEquals(1, state.rows.size)

        state.toggle("$") // re-expand
        assertEquals(countAfterInit, state.rows.size, "row count must match after collapse+expand")
    }

    @Test
    fun `expand adds children and close row`() {
        val state = stateFrom("""{"x":1,"y":2}""")

        // Root is auto-expanded in init: OPEN($), VALUE($.x), VALUE($.y), CLOSE($)
        assertEquals(4, state.rows.size)
        assertEquals(RowKind.OPEN, state.rows[0].kind)
        assertEquals(RowKind.VALUE, state.rows[1].kind)
        assertEquals(RowKind.VALUE, state.rows[2].kind)
        assertEquals(RowKind.CLOSE, state.rows[3].kind)
    }

    @Test
    fun `toggle collapsed node expands it`() {
        val state = stateFrom("""[{"a":1}]""")

        val child = "$[0]"
        assertFalse(state.isExpanded(child))

        state.toggle(child)
        assertTrue(state.isExpanded(child))
    }
}
