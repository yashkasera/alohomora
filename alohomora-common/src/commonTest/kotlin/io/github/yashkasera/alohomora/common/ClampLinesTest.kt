package io.github.yashkasera.alohomora.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClampLinesTest {

    @Test
    fun `a payload under the limit is returned unchanged`() {
        val json = "{\n  \"a\": \"1\"\n}"

        assertEquals(json, json.clampLines(6))
    }

    @Test
    fun `a payload exactly at the limit is returned unchanged`() {
        val json = (1..6).joinToString("\n") { "line$it" }

        assertEquals(json, json.clampLines(6))
    }

    @Test
    fun `a payload over the limit reports how many lines were hidden`() {
        val json = (1..10).joinToString("\n") { "line$it" }

        val clamped = json.clampLines(6)

        assertEquals(7, clamped.lines().size, "kept the wrong number of lines")
        assertTrue(clamped.startsWith("line1"))
        assertTrue(clamped.endsWith("… 4 more lines"), clamped)
    }

    @Test
    fun `an empty payload survives the clamp`() {
        assertEquals("", "".clampLines(6))
    }
}
